package com.bgssai.shortai.billing;
import com.bgssai.shortai.secret.SysSecretReader;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.production.support.ShortJson;
import org.springframework.stereotype.Component;
@Component
public class ConfiguredReceiptCredentials implements ReceiptCredentialResolver {
 private final SysSecretReader secrets;private final AiModelConfigMapper models;
 public ConfiguredReceiptCredentials(SysSecretReader secrets,AiModelConfigMapper models){this.secrets=secrets;this.models=models;}
 public String resolve(UsageReceiptRow row){
  AiModelConfigExample ex=new AiModelConfigExample();ex.createCriteria().andModelCodeEqualTo(row.model).andDeletedFlagEqualTo(0);
  for(var model:models.selectByExample(ex)){
   var cfg=ShortJson.map(model.getConfigJson());String hub=TokenHubClientSupport.resolveHub(cfg,row.model);
   String key=TokenHubClientSupport.resolveApiKey(secrets,hub,ShortJson.text(cfg.get("secret_ref")));
   if(UsageReceiptService.fingerprint(key).equals(row.keyFingerprint))return key;
  }
  for(String hub:java.util.List.of("cn","global")){
   String key=TokenHubClientSupport.resolveApiKey(secrets,hub,"");
   if(UsageReceiptService.fingerprint(key).equals(row.keyFingerprint))return key;
  }return "";
 }
}
