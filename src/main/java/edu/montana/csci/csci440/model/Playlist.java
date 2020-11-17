package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Playlist extends Model {

    Long playlistId;
    String name;

    public Playlist() {
    }

    private Playlist(ResultSet results) throws SQLException {
        name = results.getString("Name");
        playlistId = results.getLong("PlaylistId");

    }

    public List<Track> getTracks(){
        // TODO implement, order by track name
        return Track.forPlayList(this.getPlaylistId());
//        return Collections.emptyList();
    }

    public Long getPlaylistId() {
        return playlistId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static List<Playlist> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Playlist> all(int page, int count) {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM playlists LIMIT ? OFFSET ?"
             )) {
            stmt.setInt(1, count);
            stmt.setInt(2, count*page -count);
            ResultSet results = stmt.executeQuery();
            List<Playlist> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Playlist(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static Playlist find(int i) {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM playlists WHERE PlaylistId=? ")) {
            stmt.setLong(1, i);
            ResultSet results = stmt.executeQuery();
            if (results.next()) {
                return new Playlist(results);
            } else {
                return null;
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static class PlaylistTrack{

        Long playlistId;
        Long trackId;

        public PlaylistTrack() {
        }

        private PlaylistTrack(ResultSet results) throws SQLException {
            trackId = results.getLong("TrackId");
            playlistId = results.getLong("PlaylistId");

        }

        public static List<PlaylistTrack> forTrack(Long Id) {
            String query = "SELECT * FROM main.playlist_track WHERE TrackId=? ";
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setLong(1, Id);
                ResultSet results = stmt.executeQuery();
                List<PlaylistTrack> resultList = new LinkedList<>();
                System.out.println(results);
                while (results.next()) {
                    resultList.add(new PlaylistTrack(results));
                }
                return resultList;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        }
    }



}

