package cipherx;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;


public class CipherX {
    public static ArrayList<SegmentX> blockchain = new ArrayList<>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<>();

    public static int miningDifficulty = 3;
    public static float minTransactionAmount = 0.1f;
    public static CoinVault userCoinVaultA;
    public static CoinVault userCoinVaultB;
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Setup Bouncey castle as a Security Provider

        //Create wallets:
        userCoinVaultA = new CoinVault();
        userCoinVaultB = new CoinVault();
        CoinVault coinbase = new CoinVault();

        //create genesis transaction, which sends 100 NoobCoin to userWalletA:
        genesisTransaction = new Transaction(coinbase.publicKey, userCoinVaultA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction
        genesisTransaction.transactionId = "0"; //manually set the transaction id
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //manually add the Transactions Output
        UTXOs.put(genesisTransaction.outputs.getFirst().id, genesisTransaction.outputs.getFirst()); //its important to store our first transaction in the UTXOs list.

        System.out.println("Creating and Mining Genesis block... ");
        SegmentX genesis = new SegmentX("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        //testing
        SegmentX segmentX1 = new SegmentX(genesis.hash);
        System.out.println("\nuserWalletA's balance is: " + userCoinVaultA.getBalance());
        System.out.println("\nuserWalletA is Attempting to send funds (40) to userWalletB...");
        segmentX1.addTransaction(userCoinVaultA.sendFunds(userCoinVaultB.publicKey, 40f));
        addBlock(segmentX1);
        System.out.println("\nuserWalletA's balance is: " + userCoinVaultA.getBalance());
        System.out.println("userWalletB's balance is: " + userCoinVaultB.getBalance());

        SegmentX segmentX2 = new SegmentX(segmentX1.hash);
        System.out.println("\nuserWalletA Attempting to send more funds (1000) than it has...");
        segmentX2.addTransaction(userCoinVaultA.sendFunds(userCoinVaultB.publicKey, 1000f));
        addBlock(segmentX2);
        System.out.println("\nuserWalletA's balance is: " + userCoinVaultA.getBalance());
        System.out.println("userWalletB's balance is: " + userCoinVaultB.getBalance());

        SegmentX segmentX3 = new SegmentX(segmentX2.hash);
        System.out.println("\nuserWalletB is Attempting to send funds (20) to userWalletA...");
        segmentX3.addTransaction(userCoinVaultB.sendFunds( userCoinVaultA.publicKey, 20));
        System.out.println("\nuserWalletA's balance is: " + userCoinVaultA.getBalance());
        System.out.println("userWalletB's balance is: " + userCoinVaultB.getBalance());

        isChainValid();

    }

    public static void isChainValid() {
        SegmentX currentSegmentX;
        SegmentX previousSegmentX;
        String hashTarget = new String(new char[miningDifficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesisTransaction.outputs.getFirst().id, genesisTransaction.outputs.getFirst());

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentSegmentX = blockchain.get(i);
            previousSegmentX = blockchain.get(i-1);
            //compare registered hash and calculated hash:
            if(!currentSegmentX.hash.equals(currentSegmentX.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return;
            }
            //compare previous hash and registered previous hash
            if(!previousSegmentX.hash.equals(currentSegmentX.previousSegHash) ) {
                System.out.println("#Previous Hashes not equal");
                return;
            }
            //check if hash is solved
            if(!currentSegmentX.hash.substring( 0, miningDifficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return;
            }

            //loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for(int t = 0; t < currentSegmentX.transactions.size(); t++) {
                Transaction currentTransaction = currentSegmentX.transactions.get(t);

                if(currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
                    return;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                    return;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return;
                }

            }

        }
        System.out.println("Blockchain is valid");
    }

    public static void addBlock(SegmentX newSegmentX) {
        newSegmentX.mineBlock(miningDifficulty);
        blockchain.add(newSegmentX);
    }
}

