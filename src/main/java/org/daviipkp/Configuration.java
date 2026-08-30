package org.daviipkp;

class Configuration {

        private boolean support_bind = true;
        private WatchType watch_type = WatchType.WATCH_SERVICE;
        private long watch_service_delay = 1000;
        private long polling_delay = 10000;

        public Configuration() {
            //SHOULD GET VALUES FROM CONFIG.YML
        }

        public void enableBind() {
            support_bind = true;
        }

        public void disableBind() {
            support_bind = false;
        }

        public boolean canBind() {
            return support_bind;
        }

        public void setWatchType(WatchType arg0) {
            watch_type = arg0;
        }

        public WatchType getWatchType() {
            return watch_type;
        }

        public long getWatchServiceDelay() {
            return watch_service_delay;
        }

        public void setWatchWatchServiceDelay(long watch_service_delay) {
            this.watch_service_delay = watch_service_delay;
        }

        public long getPollingDelay() {
            return polling_delay;
        }

        public void setPollingDelay(long polling_delay) {
            this.polling_delay = polling_delay;
        }


    }
