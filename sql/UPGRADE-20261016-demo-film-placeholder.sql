-- Make w-dm-004 finished films playable as SVG placeholders (same pattern as w-dm-005).
UPDATE short_asset
SET mime_type = 'image/svg+xml',
    storage_key = CONCAT(
      '<svg xmlns=''http://www.w3.org/2000/svg'' width=''1080'' height=''1920'' viewBox=''0 0 1080 1920''>',
      '<rect width=''1080'' height=''1920'' fill=''#111827''/>',
      '<rect x=''80'' y=''700'' width=''920'' height=''420'' rx=''24'' fill=''#0f172a'' opacity=''0.55''/>',
      '<text x=''540'' y=''860'' text-anchor=''middle'' fill=''#fff'' font-size=''64'' font-family=''sans-serif''>',
      CASE asset_id
        WHEN 'as-dm-004-film01' THEN '开场'
        WHEN 'as-dm-004-film02' THEN '高潮'
        WHEN 'as-dm-004-film03' THEN '终局'
        ELSE '成片'
      END,
      '</text>',
      '<text x=''540'' y=''980'' text-anchor=''middle'' fill=''#e2e8f0'' font-size=''36'' font-family=''sans-serif''>成片占位 88 秒</text>',
      '<text x=''540'' y=''1780'' text-anchor=''middle'' fill=''#fff'' font-size=''28'' font-family=''sans-serif''>演示种子数据 · 非真实生成</text></svg>'
    )
WHERE asset_id IN ('as-dm-004-film01', 'as-dm-004-film02', 'as-dm-004-film03')
  AND asset_type = 'FILM'
  AND storage_key LIKE '{%';
