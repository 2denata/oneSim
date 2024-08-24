package routing;

/**
 * Class yang berisi informasi people rank node dan jumlah friendshipnya
 * 
 * @author JPD
 * @param <PR> Nilai people rank
 * @param <S> Jumlah friendship
 */
public class Informasi<PR, S> {

    private PR peopleRank;
    private S size;

    /**
     * Buat objek teman baru.
     *
     * @param peopleRank Nilai people rank dari node
     * @param size Jumlah friendship pada node
     */
    public Informasi(PR peopleRank, S size) {
        this.peopleRank = peopleRank;
        this.size = size;
    }

    /**
     * Returns the peopleRank
     *
     * @return the peopleRank
     */
    public PR getPeopleRank() {
        return peopleRank;
    }

    /**
     * Returns the size
     *
     * @return the size
     */
    public S getSize() {
        return size;
    }

    /**
     * Returns a string representation of the tuple
     *
     * @return a string representation of the tuple
     */
    public String toString() {
        return peopleRank.toString() + ":" + size.toString();
    }
}
