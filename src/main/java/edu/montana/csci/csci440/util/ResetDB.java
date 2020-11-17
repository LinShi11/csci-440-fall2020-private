package edu.montana.csci.csci440.util;

import edu.montana.csci.csci440.model.Track;
import redis.clients.jedis.Jedis;

import java.io.IOException;

public class ResetDB {
    public static void main(String[] args) throws IOException {
        DB.reset();
        try {
            Jedis jedis = new Jedis();
            jedis.del(Track.REDIS_CACHE_KEY);
            System.out.println(jedis.get(Track.REDIS_CACHE_KEY));
        } catch (Exception e) {
            System.out.println("No redis found to reset");
        }
    }
}
