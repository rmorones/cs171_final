package cs171_final;

import java.util.Objects;

/**
 *
 * @author Ricardo Morones <rmorones@umail.ucsb.edu>
 * @author Chris Kim <chriskim06@gmail.com>
 */
@SuppressWarnings("FieldMayBeFinal")
public class Pair {
    public Integer first;
    public Integer second;
    
    public Pair() {
        this.first = null;
        this.second = null;
    }
    
    public Pair(Integer first, Integer second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        Pair b = (Pair)obj;
        return this.first.compareTo(b.first) == 0 && this.second.compareTo(b.second) == 0;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.first);
        hash = 59 * hash + Objects.hashCode(this.second);
        return hash;
    }
}
