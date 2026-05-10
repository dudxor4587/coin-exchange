package com.coinexchange.trading.presentation;

import com.coinexchange.common.auth.annotation.CurrentUserId;
import com.coinexchange.order.presentation.dto.BuyOrderRequest;
import com.coinexchange.order.presentation.dto.SellOrderRequest;
import com.coinexchange.trading.application.OrderFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController {

    private final OrderFlowService orderFlowService;

    @PostMapping("/orders/buy")
    public ResponseEntity<String> buyLimitOrder(@RequestBody BuyOrderRequest request,
                                                @CurrentUserId Long userId) {
        orderFlowService.placeBuyOrder(request.coinId(), request.price(), request.amount(), userId);
        return ResponseEntity.ok("주문이 완료되었습니다.");
    }

    @PostMapping("/orders/sell")
    public ResponseEntity<String> sellLimitOrder(@RequestBody SellOrderRequest request,
                                                 @CurrentUserId Long userId) {
        orderFlowService.placeSellOrder(request.coinId(), request.price(), request.amount(), userId);
        return ResponseEntity.ok("주문이 완료되었습니다.");
    }
}
