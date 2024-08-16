package cipherx;
import java.util.ArrayList;
import java.util.Date;

public class SegmentX {
    public String hash;
    public String previousSegHash;
    public String merkleRootHash;
    public ArrayList<Transaction> transactions = new ArrayList<>(); //our data will be a simple message.
    public long timeStamp; //as number of milliseconds since 1/1/1970.
    public int nonceValue;

    //Block Constructor.
    public SegmentX(String previousSegHash ) {
        this.previousSegHash = previousSegHash;
        this.timeStamp = new Date().getTime();

        this.hash = calculateHash(); //Making sure we do this after we set the other values.
    }

    //Calculate new hash based on blocks contents
    public String calculateHash() {
        return StringOps.applySha256(
                previousSegHash +
                        (timeStamp) +
                        (nonceValue) +
                        merkleRootHash
        );
    }

    //Increases nonceValue value until hash target is reached.
    public void mineBlock(int miningDifficulty) {
        merkleRootHash = StringOps.getMerkleRoot(transactions);
        String target = StringOps.getDificultyString(miningDifficulty); //Create a string with miningDifficulty * "0"
        while(!hash.substring( 0, miningDifficulty).equals(target)) {
            nonceValue ++;
            hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + hash);
    }

    //Add transactions to this block
    public void addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if(transaction == null) return;
        if((!"0".equals(previousSegHash))) {
            if((!transaction.processTransaction())) {
                System.out.println("Transaction failed to process. Discarded.");
                return;
            }
        }

        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
    }

}
