package mochineko.natural_disaster.api;

import com.google.gson.Gson;
import mochineko.natural_disaster.Main;
import mochineko.natural_disaster.status.EarthquakeScaleType;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P2P地震APIです。
 */
public class P2PEarthquakeAPI extends DisasterAPI {

    private static BukkitTask task;

    private Earthquake earthquake;
    private Point[] points;
    private String time;

    public static P2PEarthquakeAPI[] getAPI() throws IOException {
        URL url = new URL("https://api.p2pquake.net/v2/history?codes=551");
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
        return  (new Gson()).fromJson(content.toString(), P2PEarthquakeAPI[].class);
    }

    public static void startMonitor() {
        int duration = 10;
        if (task == null) {
            Bukkit.getLogger().info("地震APIの監視を開始しました。");
            new BukkitRunnable() {
                private static LocalDateTime beforeGetTime = LocalDateTime.MIN.plusMinutes(1);
                StringBuilder builder = new StringBuilder();
                @Override
                public void run() {
                    try {
                        P2PEarthquakeAPI latestAPI = getAPI()[0];
                        Point[] pointList = latestAPI.getPoints();
                        Earthquake earthquake = latestAPI.getEarthquake();
                        Earthquake.Hypocenter hypocenter = earthquake.getHypocenter();
                        LocalDateTime nowDate = LocalDateTime.now();
                        LocalDateTime earthquakeDate = latestAPI.getLocalDateTime();
                        Bukkit.getLogger().info(Math.abs(Duration.between(nowDate, earthquakeDate).toSeconds()) + "\n" +
                                beforeGetTime + "\n" + earthquakeDate +  "\n" + beforeGetTime.isBefore(earthquakeDate));
                        if (beforeGetTime.isBefore(earthquakeDate)) {
                            if (Math.abs(Duration.between(nowDate, earthquakeDate).toSeconds()) <= 60) {
                                Bukkit.broadcastMessage("震源名：" + hypocenter.getName() + "\n" +
                                        "マグニチュード: " + hypocenter.getMagnitude() + "\n" +
                                        "深さ：" + hypocenter.getDepth());

                                builder.append("----観測情報----" + "\n");
                                for (Map.Entry<EarthquakeScaleType, List<Point>> entry : latestAPI.getScaleMap().entrySet()) {
                                    /*
                                    出力イメージ：震度3：石川県金沢市○○
                                    　　　　　　　震度2：石川県穴水町○○
                                     */

                                    EarthquakeScaleType key = entry.getKey();
                                    List<Point> value = entry.getValue();
                                    builder.append(key.getName() + ":");
                                    for (Point point : value) {
                                        builder.append(point.getPref() + point.getAddr() + ",");
                                    }
                                    builder.append("\n");
                                }
                                builder.append("------------");

                                Bukkit.broadcastMessage(builder.toString());

                                beforeGetTime = earthquakeDate;
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.runTaskTimer(Main.getPlugin(Main.class), 0L, 20L*duration);
        }
    }

    public Earthquake getEarthquake() {
        return earthquake;
    }

    public Point[] getPoints() {
        return points;
    }

    public Map<EarthquakeScaleType, List<Point>> getScaleMap() {
        Map<EarthquakeScaleType, List<Point>> map = new HashMap<>();
        for (Point point : points) {
            if (!map.containsKey(point.getScaleType())) {
                map.put(point.getScaleType(), new ArrayList<>());
            }
            List<Point> list = map.get(point.getScaleType());
            list.add(point);
        }
        return map;
    }

    public String getTime() {
        return time;
    }

    public LocalDateTime getLocalDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SS");
        return LocalDateTime.parse(time, formatter);
    }

    public static class Earthquake {
        private String domesticTsunami;
        private String foreignTsunami;
        private Hypocenter hypocenter;
        private int maxScale;
        private String time;

        /**
         * 国内への津波の有無を返す関数
         */
        public String getDomesticTsunami() {
            return domesticTsunami;
        }

        /**
         * 海外での津波の有無を返す関数
         */
        public String getForeignTsunami() {
            return foreignTsunami;
        }

        public Hypocenter getHypocenter() {
            return hypocenter;
        }

        /**
         * 最大震度を返す関数。
         * @apiNote 震度情報が存在しない場合は、-1を返す。
         */
        public int getMaxScale() {
            return maxScale;
        }

        /**
         * 最大震度を変換された形で返す関数
         */
        @Nullable
        public EarthquakeScaleType getMaxScaleType() {
            return EarthquakeScaleType.convertP2PAPI(maxScale);
        }

        /**
         * 地震の発生時刻
         */
        @Nonnull
        public String getTime() {
            return time;
        }

        public LocalDateTime getDateTime() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            return LocalDateTime.parse(time, formatter);
        }

        public static class Hypocenter {
            private int depth;
            private float latitude;
            private float longitude;
            private float magnitude;
            private String name;

            /**
             * 深さを返す関数
             */
            public int getDepth() {
                return depth;
            }

            /**
             * 緯度を返す関数
             */
            public float getLatitude() {
                return latitude;
            }

            /**
             * 経度を返す関数
             */
            public float getLongitude() {
                return longitude;
            }

            /**
             *　マグニチュードを返す関数
             */
            public float getMagnitude() {
                return magnitude;
            }

            /**
             * 震源名を返す関数
             */
            public String getName() {
                return name;
            }
        }
    }

    public static class Point {
        private String addr; //観測点名
        private String pref; //都道府県
        private int scale; //震度

        /**
         * 都道府県を返す関数
         * @return 文字列で返す（例：石川県）
         */
        public String getPref() {
            return pref;
        }

        /**
         *  観測点名を取得する関数
         * @return 文字列で返す（例：輪島市鳳至町）
         */
        public String getAddr() {
            return addr;
        }

        /**
         * 震度を生のデータで返す関数
         * @return intで返す
         */
        public int getScale() {
            return scale;
        }

        /**
         * 震度を変換されたデータで返す関数
         * @return {@link EarthquakeScaleType}　で返す
         */
        public EarthquakeScaleType getScaleType() {
            return EarthquakeScaleType.convertP2PAPI(scale);
        }

    }
}
