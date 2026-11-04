#!/bin/sh
# Renders the production configuration, then replaces this shell with Alertmanager.
set -eu

sed -e "s|@SMTP_HOST@|$SMTP_HOST|" \
    -e "s|@SMTP_PORT@|$SMTP_PORT|" \
    -e "s|@SMTP_USERNAME@|$SMTP_USERNAME|" \
    -e "s|@ALERT_EMAIL_FROM@|$ALERT_EMAIL_FROM|" \
    -e "s|@ALERT_EMAIL_TO@|$ALERT_EMAIL_TO|" \
    /etc/alertmanager/config/production.template.yml > /tmp/alertmanager.yml

exec /bin/alertmanager --config.file=/tmp/alertmanager.yml --storage.path=/alertmanager
