package com.bgssai.shortai.billing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ModelPricingTest {
 @Test void copiesRealFractionalRateAndCurrencyWithoutInferringFromModelName(){
  var actual=ModelPricing.from(java.util.Map.of("billing_multiplier",new java.math.BigDecimal("0.5"),"price_version",9,"currency","USD","api_key","secret"));
  assertEquals(new java.math.BigDecimal("0.5"),actual.get("billing_multiplier"));assertEquals(9,actual.get("price_version"));assertEquals("USD",actual.get("currency"));assertFalse(actual.containsKey("api_key"));
  var missing=ModelPricing.from(java.util.Map.of("model","gpt-5-any"));assertNull(missing.get("billing_multiplier"));assertNull(missing.get("price_version"));assertEquals("unconfigured",missing.get("pricing_status"));
 }
}
