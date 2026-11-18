-- 후원 공고 물품 진행률. 품목이 여러 개여도 공고 전체 기준 목표/현재 수량 하나로만 집계한다.
ALTER TABLE posting
  ADD COLUMN donation_target_quantity INT NULL,
  ADD COLUMN donation_received_quantity INT NULL;
