package com.bgssai.shortai.common.session;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
class SingleSessionGuardTest {
    @Test void laterLoginReplacesOnlyTheSessionAndRejectsOldOrMissingIds() {
        var mapper = mock(SysUserMapper.class);
        var row = new SysUser(); row.setStatus(true); row.setDelFlag(false);
        when(mapper.selectOneByExample(any())).thenReturn(row);
        when(mapper.updateByExampleSelective(any(), any())).thenAnswer(call -> {
            SysUser patch = call.getArgument(0);
            assertNull(patch.getPassword()); assertNull(patch.getRealName());
            row.setCurrentSessionId(patch.getCurrentSessionId()); return 1;
        });
        var guard = new SingleSessionGuard(mapper);
        String first = guard.newSessionId(), second = guard.newSessionId();
        assertNotEquals(first, second);
        guard.bindSession("user", first); assertEquals(SessionState.VALID, guard.check("user", first));
        guard.bindSession("user", second); assertEquals(SessionState.SUPERSEDED, guard.check("user", first));
        assertEquals(SessionState.VALID, guard.check("user", second));
        assertEquals(SessionState.EXPIRED, guard.check("user", null));
        row.setStatus(false); assertEquals(SessionState.EXPIRED, guard.check("user", second));
    }
    @Test void cannotIssueSessionWhenDatabaseUpdateFails() {
        var mapper = mock(SysUserMapper.class);
        var guard = new SingleSessionGuard(mapper);
        assertThrows(com.bgssai.shortai.common.exception.UnauthorizedException.class, () -> guard.bindSession("missing", guard.newSessionId()));
    }
}
