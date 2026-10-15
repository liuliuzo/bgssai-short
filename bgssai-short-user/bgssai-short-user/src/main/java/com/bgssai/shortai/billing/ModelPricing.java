package com.bgssai.shortai.billing;
import java.util.*;
public final class ModelPricing {
 private ModelPricing(){}
 public static Map<String,Object> from(Map<?,?> source){
  Map<String,Object> out=new LinkedHashMap<>();
  for(String field:List.of("billing_multiplier","billing_unit","price_version","pricing_status","currency","input_fen_per_million","output_fen_per_million","cached_read_fen_per_million","price_unit","image_fen_per_unit","audio_fen_per_second","video_fen_per_second")) out.put(field,source.get(field));
  if(out.get("pricing_status")==null)out.put("pricing_status","unconfigured");
  return out;
 }
}
