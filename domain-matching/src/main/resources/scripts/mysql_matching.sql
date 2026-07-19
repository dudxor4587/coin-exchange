-- MySQL 매칭엔진 (측정용) — 같은 폴더 matchingLogic.lua(Redis 매칭)의 대응 구현.
-- 앱은 Redis 매칭을 쓰고, 이 SQL은 DB vs Redis 성능 비교 측정에만 쓴다.
-- 오더북을 관계형 테이블에 두고, 매칭 로직을 저장 프로시저(서버사이드)로 돌린다.
-- Lua와 공정하게 비교하려고 O(N)으로 짰다: 가격별로 인덱스를 태워 한 행씩 pop(Lua의 SPOP에 대응).
-- 자세한 측정/결론: docs/매칭엔진 격리 재측정 — DB vs Redis.md

DROP TABLE IF EXISTS order_book;
CREATE TABLE order_book (
    id               BIGINT PRIMARY KEY,
    user_id          BIGINT,
    price            INT,
    remaining_amount BIGINT,
    side             VARCHAR(4),                 -- 'BUY' / 'SELL'
    INDEX idx_match (side, price, remaining_amount)
) ENGINE = InnoDB;

-- 전체 호가창을 소진하는 매칭 (batch 측정용). 체결 수를 반환한다.
-- 매칭 가능한 가격을 돌며, 각 가격에서 매수/매도를 한 행씩 pop해 min 수량으로 체결한다.
DROP PROCEDURE IF EXISTS match_all;
DELIMITER $$
CREATE PROCEDURE match_all()
BEGIN
    DECLARE cnt   BIGINT DEFAULT 0;
    DECLARE v_price INT;
    DECLARE pdone INT DEFAULT 0;
    DECLARE v_bid BIGINT; DECLARE v_sid BIGINT;
    DECLARE v_brem BIGINT; DECLARE v_srem BIGINT; DECLARE v_m BIGINT;
    DECLARE pcur CURSOR FOR
        SELECT DISTINCT b.price FROM order_book b
        WHERE b.side = 'BUY'
          AND EXISTS (SELECT 1 FROM order_book s WHERE s.side = 'SELL' AND s.price = b.price);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET pdone = 1;
    START TRANSACTION;
    OPEN pcur;
    price_loop: LOOP
        FETCH pcur INTO v_price;
        IF pdone = 1 THEN LEAVE price_loop; END IF;
        pair_loop: LOOP
            BEGIN
                DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_bid = -1;
                SET v_bid = -1;
                SELECT id, remaining_amount INTO v_bid, v_brem
                FROM order_book WHERE side = 'BUY' AND price = v_price AND remaining_amount > 0 LIMIT 1;
            END;
            BEGIN
                DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_sid = -1;
                SET v_sid = -1;
                SELECT id, remaining_amount INTO v_sid, v_srem
                FROM order_book WHERE side = 'SELL' AND price = v_price AND remaining_amount > 0 LIMIT 1;
            END;
            IF v_bid = -1 OR v_sid = -1 THEN LEAVE pair_loop; END IF;
            SET v_m = LEAST(v_brem, v_srem); SET cnt = cnt + 1;
            IF v_brem - v_m <= 0 THEN DELETE FROM order_book WHERE id = v_bid;
                ELSE UPDATE order_book SET remaining_amount = v_brem - v_m WHERE id = v_bid; END IF;
            IF v_srem - v_m <= 0 THEN DELETE FROM order_book WHERE id = v_sid;
                ELSE UPDATE order_book SET remaining_amount = v_srem - v_m WHERE id = v_sid; END IF;
        END LOOP;
    END LOOP;
    CLOSE pcur;
    COMMIT;
    SELECT cnt AS matched;
END$$
DELIMITER ;

-- 한 가격의 매칭 (per-order 측정용): 주문이 도착하면 그 가격에서 교차하는 쌍을 소진한다.
DROP PROCEDURE IF EXISTS match_price;
DELIMITER $$
CREATE PROCEDURE match_price(IN p_price INT)
BEGIN
    DECLARE v_bid BIGINT; DECLARE v_sid BIGINT;
    DECLARE v_brem BIGINT; DECLARE v_srem BIGINT; DECLARE v_m BIGINT;
    pair_loop: LOOP
        BEGIN
            DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_bid = -1;
            SET v_bid = -1;
            SELECT id, remaining_amount INTO v_bid, v_brem
            FROM order_book WHERE side = 'BUY' AND price = p_price AND remaining_amount > 0 LIMIT 1;
        END;
        BEGIN
            DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_sid = -1;
            SET v_sid = -1;
            SELECT id, remaining_amount INTO v_sid, v_srem
            FROM order_book WHERE side = 'SELL' AND price = p_price AND remaining_amount > 0 LIMIT 1;
        END;
        IF v_bid = -1 OR v_sid = -1 THEN LEAVE pair_loop; END IF;
        SET v_m = LEAST(v_brem, v_srem);
        IF v_brem - v_m <= 0 THEN DELETE FROM order_book WHERE id = v_bid;
            ELSE UPDATE order_book SET remaining_amount = v_brem - v_m WHERE id = v_bid; END IF;
        IF v_srem - v_m <= 0 THEN DELETE FROM order_book WHERE id = v_sid;
            ELSE UPDATE order_book SET remaining_amount = v_srem - v_m WHERE id = v_sid; END IF;
    END LOOP;
END$$
DELIMITER ;
