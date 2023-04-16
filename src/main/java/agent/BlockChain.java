package agent;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class BlockChain implements Serializable{

    private List<Block> blocks = new LinkedList<>();

    private Object lock = new Object();

    public BlockChain(Block root) {
        add(root);
    }

    public void add(Block block) {
        synchronized (lock) {
            blocks.add(block);
        }
    }

    public void add(List<Block> blockList) {
        synchronized (lock) {
            for(Block block: blockList) {
                boolean find = false;
                for(Block old: blocks) {
                    if(old.getHash().equals(block.getHash())) {
                        find = true;
                        break;
                    }

                }
                if(find) {
                    continue;
                }
                blocks.add(block);
            }
        }
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public Block getLatestBlock() {
        return blocks.get(blocks.size()-1);
    }

    public int size() {
        return blocks.size();
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public Object getLock() {
        return lock;
    }

    public void setLock(Object lock) {
        this.lock = lock;
    }
}
