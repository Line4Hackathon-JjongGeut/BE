-- 내 계정에 프로필 사진 URL 추가.
ALTER TABLE users
  ADD COLUMN profile_image_url VARCHAR(500) NULL AFTER phone;
