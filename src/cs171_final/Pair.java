package cs171_final;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
@SuppressWarnings("FieldMayBeFinal")
public class Pair {
    private Integer first;
    private Integer second;
    
    public Pair() {
        this.first = null;
        this.second = null;
    }
    
    public Pair(Integer first, Integer second) {
        this.first = first;
        this.second = second;
    }
}
