FROM rabbitmq:3.13.3-management

LABEL maintainer="Lion Li"

ADD ./rabbitmq_delayed_message_exchange-3.13.0.ez /plugins

RUN rabbitmq-plugins enable rabbitmq_delayed_message_exchange

ENTRYPOINT ["rabbitmq-server"]


