package agent;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ProofOfWork implements Serializable {
    private Block mBlock;
    private BigInteger mTarget;
    public static int mDiffcult;

    public ProofOfWork(Block mBlock) {
        this.mBlock = mBlock;
        mTarget = BigInteger.ONE;
        mTarget = mTarget.shiftLeft(256 - ProofOfWork.mDiffcult);
    }

    private String prePareData(int nonce) {
        String result = mBlock.getPreviousHash() +
                mBlock.getData() +
                mBlock.getTimestamp() +
                Integer.toHexString(mTarget.intValue()) +
                Integer.toHexString(nonce);
        return result;
    }


    public Map<String, String> run() {
        String hash = "";
        int nonce = 0;
        while (true) {
            String data = prePareData(nonce);
            hash = Utils.hash256(data);
            BigInteger hashInt = new BigInteger(hash, 16);
            if (hashInt.compareTo(mTarget) == -1) {
                break;
            } else {
                nonce++;
            }
        }
        String validation = mBlock.getPreviousHash() +
                mBlock.getData() +
                mBlock.getTimestamp() +
                Integer.toHexString(mTarget.intValue()) +
                Integer.toHexString(nonce);
        if (Utils.hash256(validation).equals(hash)) {
            Map<String, String> map = new HashMap<>(100);
            map.put("nonce", nonce + "");
            map.put("hash", hash);
            return map;
        } else {
            return null;
        }

    }



}
