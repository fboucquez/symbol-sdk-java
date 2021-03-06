/*
 * Copyright 2020 NEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.nem.symbol.sdk.model.transaction;

import io.nem.symbol.catapult.builders.CosignatureBuilder;
import io.nem.symbol.catapult.builders.SignatureDto;
import io.nem.symbol.core.utils.ConvertUtils;
import io.nem.symbol.sdk.infrastructure.SerializationUtils;
import io.nem.symbol.sdk.model.account.Account;
import io.nem.symbol.sdk.model.account.PublicAccount;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;

/**
 * The aggregate innerTransactions contain multiple innerTransactions that can be initiated by
 * different accounts.
 *
 * @since 1.0
 */
public class AggregateTransaction extends Transaction {

  private final String transactionsHash;

  private final List<Transaction> innerTransactions;

  private final List<AggregateTransactionCosignature> cosignatures;

  /** AggregateTransaction constructor using factory. */
  AggregateTransaction(AggregateTransactionFactory factory) {
    super(factory);
    this.transactionsHash = factory.getTransactionsHash();
    this.innerTransactions = Collections.unmodifiableList(factory.getInnerTransactions());
    this.cosignatures = Collections.unmodifiableList(factory.getCosignatures());
  }

  /**
   * Returns list of innerTransactions included in the aggregate transaction.
   *
   * @return List of innerTransactions included in the aggregate transaction.
   */
  public List<Transaction> getInnerTransactions() {
    return innerTransactions;
  }

  /**
   * Returns list of transaction cosigners signatures.
   *
   * @return List of transaction cosigners signatures.
   */
  public List<AggregateTransactionCosignature> getCosignatures() {
    return cosignatures;
  }

  /** @return Aggregate hash of an aggregate's transactions */
  public String getTransactionsHash() {
    return transactionsHash;
  }

  /**
   * Sign transaction with cosignatories creating a new SignedTransaction.
   *
   * @param initiatorAccount Initiator account
   * @param cosignatories The list of accounts that will cosign the transaction
   * @param generationHash the block 1 generation hash used to sign.
   * @return {@link SignedTransaction}
   */
  public SignedTransaction signTransactionWithCosigners(
      final Account initiatorAccount,
      final List<Account> cosignatories,
      final String generationHash) {
    SignedTransaction signedTransaction = this.signWith(initiatorAccount, generationHash);
    List<CosignatureSignedTransaction> cosignatures =
        cosignatories.stream()
            .map(cosignatory -> cosignatory.signCosignatureTransaction(signedTransaction.getHash()))
            .collect(Collectors.toList());
    return signTransactionGivenSignatures(initiatorAccount, cosignatures, generationHash);
  }

  /**
   * Sign transaction with cosignatures creating a new SignedTransaction.
   *
   * @param initiatorAccount Initiator account
   * @param cosignatures The list of precreated cosignatures.
   * @param generationHash the block 1 generation hash used to sign.
   * @return {@link SignedTransaction}
   */
  public SignedTransaction signTransactionGivenSignatures(
      final Account initiatorAccount,
      final List<? extends AggregateTransactionCosignature> cosignatures,
      final String generationHash) {
    SignedTransaction signedTransaction = this.signWith(initiatorAccount, generationHash);
    StringBuilder payload = new StringBuilder(signedTransaction.getPayload());

    for (AggregateTransactionCosignature cosignatory : cosignatures) {
      SignatureDto signature = SerializationUtils.toSignatureDto(cosignatory.getSignature());
      CosignatureBuilder builder =
          CosignatureBuilder.create(
              cosignatory.getVersion().longValue(),
              SerializationUtils.toKeyDto(cosignatory.getSigner().getPublicKey()),
              signature);
      payload.append(ConvertUtils.toHex(builder.serialize()));
    }

    byte[] payloadBytes = ConvertUtils.fromHexToBytes(payload.toString());
    byte[] size = BigInteger.valueOf(payloadBytes.length).toByteArray();
    ArrayUtils.reverse(size);
    System.arraycopy(size, 0, payloadBytes, 0, size.length);
    return new SignedTransaction(
        initiatorAccount.getPublicAccount(),
        ConvertUtils.toHex(payloadBytes),
        signedTransaction.getHash(),
        getType());
  }

  /**
   * Get the bytes required for signing.
   *
   * @param payloadBytes Payload bytes.
   * @param generationHashBytes Generation hash bytes.
   * @return Bytes to sign.
   */
  @Override
  public byte[] getSignBytes(final byte[] payloadBytes, final byte[] generationHashBytes) {
    final short headerSize = 4 + 32 + 64 + 8;
    // Aggregate tx only require to sign the body.
    final short signingBytesSize = 52;
    final byte[] signingBytes = new byte[signingBytesSize + generationHashBytes.length];
    System.arraycopy(generationHashBytes, 0, signingBytes, 0, generationHashBytes.length);
    System.arraycopy(
        payloadBytes, headerSize, signingBytes, generationHashBytes.length, signingBytesSize);
    return signingBytes;
  }

  /**
   * Check if account has signed transaction.
   *
   * @param publicAccount - Signer public account
   * @return boolean
   */
  public boolean signedByAccount(PublicAccount publicAccount) {
    return this.getSigner().filter(a -> a.equals(publicAccount)).isPresent()
        || this.getCosignatures().stream().anyMatch(o -> o.getSigner().equals(publicAccount));
  }

  /** @return the transaction is not fully loaded if there are not inner transactions. */
  @Override
  public boolean isTransactionFullyLoaded() {
    return !this.innerTransactions.isEmpty();
  }
}
