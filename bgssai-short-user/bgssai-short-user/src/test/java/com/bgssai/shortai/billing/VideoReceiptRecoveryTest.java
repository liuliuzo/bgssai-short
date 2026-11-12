package com.bgssai.shortai.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import com.bgssai.shortai.mapper.UsageReceiptMapper;

class VideoReceiptRecoveryTest {
    final UsageReceiptMapper mapper=mock(UsageReceiptMapper.class);
    final UsageReceiptService service=new UsageReceiptService(mapper,mock(ReceiptCredentialResolver.class),mock(PlatformTransactionManager.class));
    final UsageReceiptRow row=new UsageReceiptRow();
    static final String REQUEST="981ab970-447e-4a04-a2f0-a085ebbf435b";
    static final String BASE="https://dev.user.bgssai-tokenhub.cn";
    @BeforeEach void setup() {
        row.id=1L;row.userId="u";row.workRef="w";row.taskRef="t";row.model="bgssai-video";row.requestId=REQUEST;
        row.endpoint=BASE;row.keyFingerprint=UsageReceiptService.fingerprint("key");
        when(mapper.selectByExample(any())).thenReturn(List.of(row));
        UsageScope.set("u","w","t");
    }
    @AfterEach void clear(){UsageScope.clear();com.github.pagehelper.PageHelper.clearPage();}
    @Test void matchingScopeReusesDurableReceiptWithoutInsert(){
        assertSame(row,service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));
        verify(mapper,never()).insertSelective(any());
    }
    @Test void missingOrMultipleReceiptsFailClosed(){
        when(mapper.selectByExample(any())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));
        when(mapper.selectByExample(any())).thenReturn(List.of(row,row));
        assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));
    }
    @Test void changedOwnerWorkTaskModelAndDeletionAreRejected(){
        row.userId="other";assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));row.userId="u";
        row.workRef="other";assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));row.workRef="w";
        row.taskRef="other";assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));row.taskRef="t";
        row.model="other";assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));row.model="bgssai-video";
        row.delFlag=1;assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));
    }
    @Test void requestEndpointOrKeyChangesNeverFallBackToAnotherCredential(){
        assertThrows(IllegalArgumentException.class,()->service.resumeVideo("other",BASE,"key","bgssai-video"));
        assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,"https://www.bgssai-tokenhub.cn","key","bgssai-video"));
        assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"rotated-key","bgssai-video"));
        UsageScope.clear();assertThrows(IllegalArgumentException.class,()->service.resumeVideo(REQUEST,BASE,"key","bgssai-video"));
    }
}
