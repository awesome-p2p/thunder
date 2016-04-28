package network.thunder.core.helper.wallet;

import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.*;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.WalletTransaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MockWallet extends Wallet {

    public static boolean USE_REAL_TRANSACTION = false;

    List<ECKey> keyList = new ArrayList<>();
    List<TransactionOutput> outputs = new ArrayList<>();

    public MockWallet (NetworkParameters params) {
        this(params, 100);
    }

    public MockWallet (NetworkParameters params, int totalOutputs) {
        super(params);
        //
        Random random = new Random();

        for (int i = 1; i < 101; i++) {
            Transaction transaction = new Transaction(Constants.getNetwork());

            byte[] h = new byte[32];
            random.nextBytes(h);

            ECKey k = new ECKey(new BigInteger(h), null, true);
            keyList.add(k);

            if (!USE_REAL_TRANSACTION) {
                transaction.addInput(Sha256Hash.wrap(h), 0, Tools.getDummyScript());

                TransactionOutput a = new TransactionOutput(Constants.getNetwork(), transaction, Coin.valueOf(1000000), k.toAddress(Constants.getNetwork()));
                transaction.addOutput(a);

                outputs.add(a);
            }
        }

        //If you want to test on testnet send to one of addresses of the keys above and add the raw tx here using
        //http://tbtc.blockr.io/api/v1/tx/raw/b9d8ce4e23caa339365e8a7280915dfde1b679c6e157b222ff5e527f2f0dd23d
        if (USE_REAL_TRANSACTION) {
            Transaction t = new Transaction(Constants.getNetwork(), Tools.hexStringToByteArray
                    ("0100000001c82e9ddb4cd1c54507f9c169fa7a989eb769f96cf5418bedc99f873b9b040fc4010000006a47304402206265f65016c51b7f6f51da74570bec59f7e8b1df00a3ccaf07985b29bb3a731102205baa0bea677aad450e708d3f25016f2ef8bba66f7e8664ea53cc5137e868c7270121036ea96c418eae7c6a1af51eb3c5ddd86abe1f810ee3a66fa10733d3d69b6af19dffffffff02102700000000000017a91494fc79ff226c6eee8b9e96abd5beb4cb0d34427087b06e5c00000000001976a914651dba4d544bdf48be9698eee840a6bb3a49092788ac00000000"));
            outputs.add(t.getOutput(1));
        }
    }

    public MockWallet (Context context) {
        super(context);
    }

    public MockWallet (NetworkParameters params, KeyChainGroup keyChainGroup) {
        super(params, keyChainGroup);
    }

    public MockWallet (Context context, KeyChainGroup keyChainGroup) {
        super(context, keyChainGroup);
    }

    @Override
    public List<TransactionOutput> calculateAllSpendCandidates (boolean excludeImmatureCoinbases, boolean excludeUnsignable) {
        return outputs;
    }

    @Override
    public Coin getBalance () {
        final long[] value = {0};
        this.outputs.stream().forEach(transactionOutput -> value[0] += transactionOutput.getValue().value);
        return Coin.valueOf(value[0]);
    }

    @Override
    public void addWalletTransaction (WalletTransaction wtx) {
        wtx.getTransaction().getInputs().stream().forEach(new Consumer<TransactionInput>() {
            @Override
            public void accept (TransactionInput transactionInput) {
                outputs = outputs.stream().filter(new Predicate<TransactionOutput>() {
                    @Override
                    public boolean test (TransactionOutput transactionOutput) {
                        return !transactionInput.getOutpoint().equals(transactionOutput.getOutPointFor());
                    }
                }).collect(Collectors.toList());
            }
        });
        wtx.getTransaction().getOutputs().forEach(transactionOutput ->
        {
            Address address = transactionOutput.getAddressFromP2PKHScript(Constants.getNetwork());
            if (address != null) {
                if (keyList.stream().anyMatch(
                        ecKey -> ecKey.toAddress(Constants.getNetwork()).equals(address))) {
                    outputs.add(transactionOutput);
                }
            } else {

            }

        });

    }

    @Override
    public ECKey findKeyFromPubHash (byte[] pubkeyHash) {
        for (ECKey k : keyList) {
            if (Arrays.equals(pubkeyHash, k.toAddress(Constants.getNetwork()).getHash160())) {
                return k;
            }
        }
        return null;
    }

    @Override
    public boolean addKey (ECKey key) {
        keyList.add(key);
        return true;
    }

    @Override
    public Address freshReceiveAddress () {
        return keyList.get(0).toAddress(Constants.getNetwork());
    }
}
