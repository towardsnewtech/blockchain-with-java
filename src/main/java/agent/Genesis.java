package agent;

public class Genesis extends Block {

    public Genesis() {
        this.index = 0;
        this.previousHash = "0000000000000000000000000000000000000000000000000000000000000000";
        this.creator = "ROOT";
        this.timestamp = 0L;
        this.nonce = 0;
        this.hash = Utils.hash256("Genesis");
    }

}
