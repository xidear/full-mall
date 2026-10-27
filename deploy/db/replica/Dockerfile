FROM postgres:17-alpine
COPY replica.sh /usr/local/bin/replica.sh
RUN chmod +x /usr/local/bin/replica.sh
ENTRYPOINT ["/usr/local/bin/replica.sh"]
