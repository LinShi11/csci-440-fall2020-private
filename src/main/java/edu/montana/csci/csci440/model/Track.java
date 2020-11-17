package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Track extends Model {

    private Long trackId;
    private Long albumId;
    private Long mediaTypeId;
    private Long genreId;
    private String name;
    private Long milliseconds;
    private Long bytes;
    private BigDecimal unitPrice;
    private String title;
    private String artistName;
    private static Long redisCount = -1l;

    public static final String REDIS_CACHE_KEY = "cs440-tracks-count-cache";

    public Track() {
        // new track for insert
        mediaTypeId = 1l;
        genreId = 1l;
        milliseconds  = 0l;
        bytes  = 0l;
        unitPrice = new BigDecimal("0");
    }

    public Track(ResultSet results){
        try {
            name = results.getString("Name");
            milliseconds = results.getLong("Milliseconds");
            bytes = results.getLong("Bytes");
            unitPrice = results.getBigDecimal("UnitPrice");
            trackId = results.getLong("TrackId");
            albumId = results.getLong("AlbumId");
            mediaTypeId = results.getLong("MediaTypeId");
            genreId = results.getLong("GenreId");
        }catch(SQLException e){

        }
        try{
            title = results.getString("Title");
            artistName = results.getString("ArtistName");
        }catch(SQLException e){

        }
    }

    public static Track find(long i) {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT *, a.Title, a2.Name as ArtistName FROM tracks JOIN albums a on a.AlbumId = tracks.AlbumId JOIN artists a2 on a2.ArtistId = a.ArtistId WHERE TrackId=?")) {
            stmt.setLong(1, i);
            ResultSet results = stmt.executeQuery();
            if (results.next()) {
                return new Track(results);
            } else {
                return null;
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static Long count() {
        Jedis redisClient = new Jedis(); // use this class to access redis and create a cache
        Long count = 0l;
        String counted = redisClient.get(REDIS_CACHE_KEY);
        try {
            count = Long.parseLong(counted);
        } catch(NumberFormatException ex){

        }
        if(redisCount.equals(count)){
            return count;
        }
        else{
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as Count FROM tracks")) {
                ResultSet results = stmt.executeQuery();
                if (results.next()) {
                    count = results.getLong("Count");
                    redisClient.set(REDIS_CACHE_KEY, Long.toString(count));
                    redisCount = count;
                    return count;
                } else {
                    throw new IllegalStateException("Should find a count!");
                }
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        }
    }

    public Album getAlbum() {
        return Album.find(albumId);
    }

    public MediaType getMediaType() {
        return MediaType.forTrack(this.getTrackId());
    }

    public Genre getGenre() {
        return Genre.find(this.getGenreId());
//        return null;
    }

    public List<Playlist.PlaylistTrack> getPlaylists(){
        return Playlist.PlaylistTrack.forTrack(this.getTrackId());
    }

    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(Long milliseconds) {
        if(milliseconds != null) {
            this.milliseconds = milliseconds;
        }
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        if(bytes != null) {
            this.bytes = bytes;
        }
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if(unitPrice != null) {
            this.unitPrice = unitPrice;
        }
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public void setAlbum(Album album) {
        albumId = album.getAlbumId();
    }

    public Long getMediaTypeId() {
        return mediaTypeId;
    }

    public void setMediaTypeId(Long mediaTypeId) {
        this.mediaTypeId = mediaTypeId;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public String getArtistName() {
        // TODO implement more efficiently
        //  hint: cache on this model object
        return this.artistName;
    }

    public String getAlbumTitle() {
        // TODO implement more efficiently
        //  hint: cache on this model object
        return this.title;
    }

    public static List<Track> advancedSearch(int page, int count,
                                             String search, Integer artistId, Integer albumId,
                                             Integer maxRuntime, Integer minRuntime) {
        LinkedList<Object> args = new LinkedList<>();

        String query = "SELECT * FROM tracks " +
                "JOIN albums ON tracks.AlbumId = albums.AlbumId " +
                "WHERE name LIKE ?";
        args.add("%" + search + "%");

        // Conditionally include the query and argument
        if (artistId != null) {
            query += " AND ArtistId=? ";
            args.add(artistId);
        }

        if(albumId != null){
            query += " AND Album= ? ";
            args.add(albumId);
        }

        if(minRuntime != null){
            query += " AND Milliseconds >= ? ";
            args.add(minRuntime);
        }

        if(maxRuntime != null){
            query += " AND Milliseconds <= ? ";
            args.add(maxRuntime);
        }

        query += " LIMIT ? OFFSET ?";
        args.add(count);
        args.add(page*count - count);

        if(albumId != null){
            query += " AND Album= ? ";
            args.add(albumId);
        }

        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);
                stmt.setObject(i + 1, arg);
            }
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static List<Track> search(int page, int count, String orderBy, String search) {
        List<Track> resultList = new LinkedList<>();
        String query = "SELECT *, a.Title, a2.Name as ArtistName FROM tracks t JOIN albums a on a.AlbumId = t.AlbumId JOIN artists a2 on a2.ArtistId = a.ArtistId WHERE " +
                "(ArtistName Like ? OR a.Title LIKE ? OR t.Name LIKE ?) order by "+ orderBy + " LIMIT ? OFFSET ? ";
        search = "%" + search + "%";
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, search);
            stmt.setString(2, search);
            stmt.setString(3, search);
            stmt.setInt(4, count);
            stmt.setInt(5, count*page -count);
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                resultList.add(new Track(results));
            }

        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
        return resultList;
    }

    public static List<Track> forAlbum(Long albumId) {
        String query = "SELECT * FROM tracks WHERE AlbumId=?";
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, albumId);
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public static List<Track> forPlayList(Long Id) {
        String query = "SELECT * FROM tracks t JOIN playlist_track pt on t.TrackId = pt.TrackId JOIN playlists p on p.PlaylistId = pt.PlaylistId WHERE p.PlaylistId =? ORDER BY t.Name ";
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, Id);
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    // Sure would be nice if java supported default parameter values
    public static List<Track> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Track> all(int page, int count) {
        return all(page, count, "TrackId");
    }

    public static List<Track> all(int page, int count, String orderBy) {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT *, a.Title, a2.Name as ArtistName FROM tracks t JOIN albums a on a.AlbumId = t.AlbumId " +
                             "JOIN artists a2 on a2.ArtistId = a.ArtistId order by " + orderBy + " LIMIT ? OFFSET ? "
             )) {
            stmt.setInt(1, count);
            stmt.setInt(2, count*page -count);
            ResultSet results = stmt.executeQuery();
            List<Track> resultList = new LinkedList<>();
            while (results.next()) {
                resultList.add(new Track(results));
            }
            return resultList;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    @Override
    public boolean verify() {
        _errors.clear(); // clear any existing errors
        if (name == null || "".equals(name)) {
            addError("Name can't be null or blank!");
        }
        if (albumId == null) {
            addError("Album can't be null!");
        }
        return !hasErrors();
    }

    @Override
    public boolean create() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO tracks (Name, AlbumId, MediaTypeId, GenreId, Milliseconds, Bytes, UnitPrice) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getAlbumId());
                stmt.setLong(3, this.getMediaTypeId());
                stmt.setLong(4, this.getGenreId());
                stmt.setLong(5, this.getMilliseconds());
                stmt.setLong(6, this.getBytes());
                stmt.setBigDecimal(7, this.getUnitPrice());
                stmt.executeUpdate();
                trackId = DB.getLastID(conn);
                redisCount = -1l;
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean update() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE tracks SET Name=? WHERE trackId=?")) {
                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getTrackId());
                stmt.executeUpdate();
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    @Override
    public void delete() {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM tracks WHERE TrackId=?")) {
            stmt.setLong(1, this.getTrackId());
            stmt.executeUpdate();
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

}
