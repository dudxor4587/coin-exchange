package com.coinexchange.order.presentation;

import com.coinexchange.auth.annotation.CurrentUserId;
import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.presentation.dto.BuyOrderRequest;
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

    private final OrderService orderService;

    @PostMapping("/orders/buy")
    public ResponseEntity<String> buyLimitOrder(@RequestBody BuyOrderRequest request,
                                                @CurrentUserId Long userId) {
        orderService.createBuyOrder(
                request.coinId(),
                request.price(),
                request.amount(),
                userId
        );
        return ResponseEntity.ok("주문이 완료되었습니다.");
    }
}
