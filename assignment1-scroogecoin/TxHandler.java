import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class TxHandler {
    private UTXOPool uPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        uPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        
        UTXOPool uniqueUtxos = new UTXOPool();
        double inputsValues = 0;
        for (int i = 0; i < tx.numInputs(); i++) {

            Transaction.Input ip = tx.getInput(i);
            UTXO utxo = new UTXO(ip.prevTxHash, ip.outputIndex);
            Transaction.Output output = uPool.getTxOutput(utxo);

            if (!uPool.contains(utxo)) return false;

            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), ip.signature)) 
                return false;

            if (uniqueUtxos.contains(utxo)) return false;

            uniqueUtxos.addUTXO(utxo, output);
            inputsValues += output.value;
        }

        double outputsValues = 0;
        for (Transaction.Output op: outputs) {
            if (op.value < 0) return false;
            outputsValues += op.value;
        }

        return inputsValues >= outputsValues;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for(Transaction tx: possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);
                for (Transaction.Input ip: tx.getInputs()) {
                    UTXO u = new UTXO(ip.prevTxHash, ip.outputIndex);
                    this.uPool.removeUTXO(u);
                }
                for (int i = 0; i < tx.numOutputs(); i ++) {
                    Transaction.Output txOut = tx.getOutput(i);
                    UTXO u = new UTXO(tx.getHash(), i);
                    this.uPool.addUTXO(u, txOut);
                }
            }
        }

        Transaction[] validTxArray = new Transaction[validTxs.size()];
        return validTxs.toArray(validTxArray);
    }

}
