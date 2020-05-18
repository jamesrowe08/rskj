/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 * (derived from ethereumJ library, Copyright (c) 2016 <ether.camp>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.ethereum.vm.program.invoke;

import co.rsk.core.Coin;
import co.rsk.core.RskAddress;
import co.rsk.db.MutableTrieImpl;
import co.rsk.trie.Trie;
import org.ethereum.core.Repository;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.db.BlockStoreDummy;
import org.ethereum.db.MutableRepository;
import org.ethereum.vm.DataWord;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * @author Roman Mandeleil
 * @since 03.06.2014
 */
// #mish ProgramInvoke extends InvokeData which has getGas and getRentGas methods.. also getGasLimit for block gas limit
// ToDo: the usage here appears to use gasLimit for both TX gas limti as well as block. Perhaps it is based on type (long Vs Dataword) 
// in particular the methods setGas() and setGasLimit() both target the same field..  
public class ProgramInvokeMockImpl implements ProgramInvoke {

    private byte[] msgData;

    private DataWord txindex;

    private Repository repository;
    private RskAddress ownerAddress = new RskAddress("cd2a3d9f938e13cd947ec05abc7fe734df8dd826");
    private final RskAddress defaultContractAddress = new RskAddress("471fd3ad3e9eeadeec4608b92d16ce6b500704cc");

    private RskAddress contractAddress;
    // default for most tests. This can be overwritten by the test
    private long gasLimit = 1000000;
    private long rentGasLimit = 1000000;

    public ProgramInvokeMockImpl(byte[] msgDataRaw) {
        this();
        this.msgData = msgDataRaw;
    }

    public ProgramInvokeMockImpl(String contractCode, RskAddress contractAddress) {
        this(Hex.decode(contractCode), contractAddress);
    }

    public ProgramInvokeMockImpl(byte[] contractCode, RskAddress contractAddress) {
        this.repository = new MutableRepository(new MutableTrieImpl(null, new Trie()));

        this.repository.createAccount(ownerAddress);
        this.repository.addBalance(ownerAddress, new Coin(BigInteger.valueOf(1000)));
        //Defaults to defaultContractAddress constant defined in this mock
        this.contractAddress = contractAddress!=null?contractAddress:this.defaultContractAddress;
        this.repository.createAccount(this.contractAddress);
        this.repository.setupContract(this.contractAddress);
        this.repository.saveCode(this.contractAddress, contractCode);
        this.txindex = DataWord.ZERO;
    }

    public void addAccount(RskAddress accountAddress,Coin balance) {
        this.repository.createAccount(accountAddress);
        this.repository.addBalance(accountAddress, balance);
    }

    public ProgramInvokeMockImpl() {
        this("385E60076000396000605f556014600054601e60"
                + "205463abcddcba6040545b51602001600a525451"
                + "6040016014525451606001601e52545160800160"
                + "28525460a052546016604860003960166000f260"
                + "00603f556103e75660005460005360200235", null);
    }

    public RskAddress getContractAddress() {
        return this.contractAddress;
    }

    public ProgramInvokeMockImpl(boolean defaults) {
    }

    /*           ADDRESS op         */
    public DataWord getOwnerAddress() {
        return DataWord.valueOf(ownerAddress.getBytes());
    }

    /*           BALANCE op         */
    public DataWord getBalance() {
        byte[] balance = Hex.decode("0DE0B6B3A7640000");
        return DataWord.valueOf(balance);
    }

    /*           ORIGIN op         */
    public DataWord getOriginAddress() {

        byte[] cowPrivKey = HashUtil.keccak256("horse".getBytes(StandardCharsets.UTF_8));
        byte[] addr = ECKey.fromPrivate(cowPrivKey).getAddress();

        return DataWord.valueOf(addr);
    }

    /*           CALLER op         */
    public DataWord getCallerAddress() {

        byte[] cowPrivKey = HashUtil.keccak256("monkey".getBytes(StandardCharsets.UTF_8));
        byte[] addr = ECKey.fromPrivate(cowPrivKey).getAddress();

        return DataWord.valueOf(addr);
    }

    /*           GASPRICE op       */
    public DataWord getMinGasPrice() {

        byte[] minGasPrice = Hex.decode("09184e72a000");
        return DataWord.valueOf(minGasPrice);
    }

    /*           GAS op       */
    public long  getGas() {

        return gasLimit;
    }

    /*           RENTGAS op       */
    public long  getRentGas() {

        return rentGasLimit;
    }

    public void setGas(long rentGasLimit) {
        this.rentGasLimit = rentGasLimit;
    }

    /*          CALLVALUE op    */
    public DataWord getCallValue() {
        byte[] balance = Hex.decode("0DE0B6B3A7640000");
        return DataWord.valueOf(balance);
    }

    /*****************/
    /***  msg data ***/
    /**
     * *************
     */

    /*     CALLDATALOAD  op   */
    public DataWord getDataValue(DataWord indexData) {

        byte[] data = new byte[32];

        int index = indexData.value().intValue();
        int size = 32;

        if (msgData == null) return DataWord.valueOf(data);
        if (index > msgData.length) return DataWord.valueOf(data);
        if (index + 32 > msgData.length) size = msgData.length - index;

        System.arraycopy(msgData, index, data, 0, size);

        return DataWord.valueOf(data);
    }

    /*  CALLDATASIZE */
    public DataWord getDataSize() {

        if (msgData == null || msgData.length == 0) return DataWord.valueOf(new byte[32]);
        int size = msgData.length;
        return DataWord.valueOf(size);
    }

    /*  CALLDATACOPY */
    public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {

        int offset = offsetData.value().intValue();
        int length = lengthData.value().intValue();

        byte[] data = new byte[length];

        if (msgData == null) return data;
        if (offset > msgData.length) return data;
        if (offset + length > msgData.length) length = msgData.length - offset;

        System.arraycopy(msgData, offset, data, 0, length);

        return data;
    }

    @Override
    public DataWord getPrevHash() {
        byte[] prevHash = Hex.decode("961CB117ABA86D1E596854015A1483323F18883C2D745B0BC03E87F146D2BB1C");
        return DataWord.valueOf(prevHash);
    }

    @Override
    public DataWord getCoinbase() {
        byte[] coinBase = Hex.decode("E559DE5527492BCB42EC68D07DF0742A98EC3F1E");
        return DataWord.valueOf(coinBase);
    }

    @Override
    public DataWord getTimestamp() {
        long timestamp = 1401421348;
        return DataWord.valueOf(timestamp);
    }

    @Override
    public DataWord getNumber() {
        long number = 33;
        return DataWord.valueOf(number);
    }

    @Override
    public DataWord getTransactionIndex() {
        return this.txindex;
    }

    public void setTransactionIndex(DataWord txindex) {
        this.txindex = txindex;
    }

    @Override
    public DataWord getDifficulty() {
        byte[] difficulty = Hex.decode("3ED290");
        return DataWord.valueOf(difficulty);
    }

    // #mish check this is Block gas limt.. then what are we setting with setGas()?
    @Override
    public DataWord getGaslimit() {
        return DataWord.valueOf(gasLimit);
    }


    public void setGasLimit(long gasLimit) {
        this.gasLimit = gasLimit;
    }

    public void setOwnerAddress(RskAddress ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    @Override
    public boolean byTransaction() {
        return true;
    }

    @Override
    public boolean isStaticCall() {
        return false;
    }

    @Override
    public boolean byTestingSuite() {
        return false;
    }

    @Override
    public Repository getRepository() {
        return this.repository;
    }

    @Override
    public BlockStore getBlockStore() {
        return new BlockStoreDummy();
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public int getCallDeep() {
        return 0;
    }
}
