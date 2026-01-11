package mochineko.natural_disaster.api;

import com.google.gson.Gson;
import mochineko.natural_disaster.Main;
import mochineko.natural_disaster.status.EarthquakeScaleType;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class P2PEEWAPI extends DisasterAPI {

    private static BukkitRunnable task;

    private Area[] areas;
    private Earthquake earthquake;

    public static P2PEEWAPI[] getAPI() throws IOException {
        URL url = new URL("https://api.p2pquake.net/v2/history?codes=556");
        HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();

        String inputLine;
        while((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        con.disconnect();
        return  (new Gson()).fromJson(content.toString(), P2PEEWAPI[].class);
    }

    public static void startMonitor() {
        int duration = 10;
        if (task == null) {
            Bukkit.getLogger().info("緊急地震速報（警報）の監視を開始しました。");
            new BukkitRunnable() {
                private LocalDateTime beforeGetTime = LocalDateTime.MIN.plusSeconds(duration);
                static StringBuilder builder = new StringBuilder();
                @Override
                public void run() {
                    try {
                        P2PEEWAPI latestAPI = getAPI()[0];
                        Earthquake earthquake = latestAPI.getEarthquake();
                        LocalDateTime eew_dateTime = earthquake.getOriginLocalDateTime();

                        if (beforeGetTime.isBefore(eew_dateTime)) {
                            if (Math.abs(Duration.between(LocalDateTime.now(), eew_dateTime).toSeconds()) <= 60) {

                                for (Area area : latestAPI.getAreas()) {

                                    Bukkit.broadcastMessage("緊急地震速報　警報　強い揺れに警戒してください\n" +
                                            "==========" + "\n" +
                                            area.getName() + "\n" +
                                            "========");
                                }
                                beforeGetTime = eew_dateTime;
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.runTaskTimer(Main.getPlugin(Main.class), 0L, 20L*duration);
        }
    }

    @Nullable
    public Area[] getAreas() {
        return areas;
    }

    @Nullable
    public Earthquake getEarthquake() {
        return earthquake;
    }

    /**
     * 緊急地震速報のエリアのクラス
     */
    public static class Area {
        private String name;
        private String pref;
        private int scaleFrom;
        private int scaleTo;
        private String arrivalTime;

        /**
         * 地域名を返す関数
         * @return （例：岩手県内陸北部）
         */
        @Nonnull
        public String getName() {
            return name;
        }

        /**
         * 府県予報区を返す関数
         * @return （例：岩手）
         */
        @Nonnull
        public String getPref() {
            return pref;
        }

        /**
         * 予測震度の最小震度を、生のデータで返す関数。
         * @return （例：10）
         */
        public int getScaleFrom() {
            return scaleFrom;
        }

        /**
         * 予想震度の最小震度を、列挙型で返す関数。
         * @return （例：10の場合は、震度1({@link EarthquakeScaleType#ONE})を返す）
         */
        @Nonnull
        public EarthquakeScaleType getScaleType() {
            return EarthquakeScaleType.convertP2PAPI(scaleFrom);
        }

        /**
         * 予想震度の最大震度を、生のデータで返す関数。
         * @return （例：10）
         */
        public int getScaleTo() {
            return scaleTo;
        }

        /**
         * 主要動の到達予想時刻を返す関数。
         * @return yyyy/MM/dd HH:mm:ss で返す。（例：2026/01/11 13:15:00）
         */
        @Nullable
        public String getArrivalTime() {
            return arrivalTime;
        }
    }

    public static class Earthquake {
        private String arrivalTime;
        private Hypocenter hypocenter;
        private String originTime;

        /**
         * 地震の発現の時刻
         * @return
         */
        @Nonnull
        public String getArrivalTime() {
            return arrivalTime;
        }

        /**
         * 震源の情報を取得する関数
         */
        @Nonnull
        public Hypocenter getHypocenter() {
            return hypocenter;
        }

        /**
         * 地震の発生時刻を返す関数。
         * @return yyyy/MM/dd HH:mm:ss で返す。（例：2026/01/11 13:15:00）
         * @apiNote {@link #getOriginLocalDateTime()} でLocalDateTimeに変換されたものを取得できる。
         */
        @Nonnull
        public String getOriginTime() {
            return originTime;
        }

        /**
         * 地震の発生時刻を返す関数
         * @return LocalDateTimeで返す。
         */
        @Nonnull
        public LocalDateTime getOriginLocalDateTime() {
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            return LocalDateTime.parse(originTime, timeFormat);
        }

        /**
         * 震源の情報のクラス
         */
        public static class Hypocenter {
            private String name;
            private String reduceName;
            private int depth;
            private float latitude;
            private float longitude;
            private float magnitude;

            /**
             * 震央の名前を返す関数
             * @return （例：岩手県沿岸北部）
             */
            @Nullable
            public String getName() {
                return name;
            }

            /**
             * 震央の短い名前を返す関数
             * @return （例：岩手県）
             */
            @Nullable
            public String getReduceName() {
                return reduceName;
            }

            /**
             * 深さを返す関数
             * @return int型で返す
             */
            public int getDepth() {
                return depth;
            }

            /**
             * 緯度を返す関数
             * @return float型で返す
             */
            public float getLatitude() {
                return latitude;
            }

            /**
             * 経度を返す関数
             * @return float型で返す
             */
            public float getLongitude() {
                return longitude;
            }

            /**
             * マグニチュードを返す関数
             * @return float型で返す
             */
            public float getMagnitude() {
                return magnitude;
            }
        }
    }

}
