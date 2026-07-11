-- Key 목록 (Spring에서 전달)
-- KEYS[1]: 'prices:BUY' (모든 매수 주문의 가격 Set)
-- KEYS[2]: 'prices:SELL' (모든 매도 주문의 가격 Set)

-- 1. 매칭 가능한 가격 목록을 찾음 (SINTER: 두 Set의 교집합을 반환)
local matching_prices = redis.call('SINTER', KEYS[1], KEYS[2])

-- 매칭 가능한 가격이 없으면 즉시 종료
if #matching_prices == 0 then
    return {}
end

local trades = {}
local trade_count_limit = 100

-- 2. 매칭 가능한 가격들만 순회
for _, price in ipairs(matching_prices) do
    local buy_set_key = 'orderbook:BUY:' .. price
    local sell_set_key = 'orderbook:SELL:' .. price

    -- 3. 해당 가격의 주문들을 계속해서 매칭
    while true do
        if #trades >= trade_count_limit then
            return trades
        end

        local buy_order_id = redis.call('SPOP', buy_set_key)
        local sell_order_id = redis.call('SPOP', sell_set_key)

        if not buy_order_id or not sell_order_id then
            if buy_order_id then redis.call('SADD', buy_set_key, buy_order_id) end
            if sell_order_id then redis.call('SADD', sell_set_key, sell_order_id) end
            break
        end

        local buy_order_key = 'orderbook:' .. buy_order_id
        local sell_order_key = 'orderbook:' .. sell_order_id

        local buy_order_info = redis.call('HMGET', buy_order_key, 'userId', 'remainingAmount', 'coinId')
        local sell_order_info = redis.call('HMGET', sell_order_key, 'userId', 'remainingAmount')

        if not buy_order_info[1] or not buy_order_info[2] or not buy_order_info[3] or not sell_order_info[1] or not sell_order_info[2] then
            redis.log(redis.LOG_WARNING, "Incomplete hash data for IDs: " .. buy_order_id .. ", " .. sell_order_id)
            redis.call('SADD', buy_set_key, buy_order_id)
            redis.call('SADD', sell_set_key, sell_order_id)
            break
        end

        local buy_rem = tonumber(buy_order_info[2])
        local sell_rem = tonumber(sell_order_info[2])

        local matched_amount = math.min(buy_rem, sell_rem)
        local buy_remaining = buy_rem - matched_amount
        local sell_remaining = sell_rem - matched_amount

        -- ############### [수정됨] 반환 데이터 구조 변경 ###############
        -- 기존의 map 형태 대신, key-value 쌍의 list(배열) 형태로 데이터를 삽입합니다.
        -- 이렇게 하면 Java(Spring Data Redis)에서 데이터를 안정적으로 받을 수 있습니다.
        table.insert(trades, {
            "buyOrderId", buy_order_id,
            "sellOrderId", sell_order_id,
            "buyerId", buy_order_info[1],
            "sellerId", sell_order_info[1],
            -- Lua의 숫자는 기본적으로 double 타입이므로 문자열로 변환하여 타입 일관성을 유지합니다.
            "matchedAmount", tostring(matched_amount),
            "price", price,
            "coinId", buy_order_info[3]
        })
        -- #############################################################

        if buy_remaining <= 0 then
            redis.call('DEL', buy_order_key)
        else
            redis.call('HSET', buy_order_key, 'remainingAmount', buy_remaining)
            redis.call('SADD', buy_set_key, buy_order_id)
        end

        if sell_remaining <= 0 then
            redis.call('DEL', sell_order_key)
        else
            redis.call('HSET', sell_order_key, 'remainingAmount', sell_remaining)
            redis.call('SADD', sell_set_key, sell_order_id)
        end
    end
end

return trades
