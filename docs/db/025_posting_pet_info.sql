-- 임시보호(FOSTER) 공고 상세에 "아이 정보"(이름/나이/성별/특징)를 보여주기 위한 컬럼 추가.

ALTER TABLE posting
  ADD COLUMN pet_name VARCHAR(50) NULL AFTER external_url,
  ADD COLUMN pet_age_text VARCHAR(50) NULL AFTER pet_name,
  ADD COLUMN pet_gender VARCHAR(10) NULL COMMENT 'MALE / FEMALE' AFTER pet_age_text,
  ADD COLUMN pet_characteristics VARCHAR(500) NULL AFTER pet_gender;
