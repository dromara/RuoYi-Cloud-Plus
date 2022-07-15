FROM rabbitmq:3.10.6-management

MAINTAINER Lion Li

ADD ./rabbitmq_delayed_message_exchange-3.10.2.ez /plugins

RUN rabbitmq-plugins enable rabbitmq_delayed_message_exchange

ENTRYPOINT ["rabbitmq-server"]


