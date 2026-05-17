package com.coinexchange.funds.infra.outbox;

/** outbox에 row가 INSERT됐을 때 발행되는 내부 시그널. relay를 깨우는 용도. */
public record OutboxInsertedSignal() {}
