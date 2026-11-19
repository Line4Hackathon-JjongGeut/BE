-- 보호시설 상세에 운영시간/방문안내를 보여주기 위한 컬럼 추가.

ALTER TABLE shelter
  ADD COLUMN operating_hours VARCHAR(100) NULL AFTER longitude,
  ADD COLUMN visit_policy VARCHAR(100) NULL AFTER operating_hours;
