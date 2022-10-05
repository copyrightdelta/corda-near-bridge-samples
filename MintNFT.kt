package com.copyrightdelta.drx.flows.music.splits

import co.paralleluniverse.fibers.Suspendable
import com.copyrightdelta.drx.contracts.music.splits.contracts.DigitalRightsTokenContract
import com.copyrightdelta.drx.contracts.music.splits.states.TokenLockState
import com.copyrightdelta.drx.flows.account.UUIDToAccount
import com.copyrightdelta.drx.flows.util.NotaryConf
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*


@CordaSerializable
data class MintNFTParams(
    val me: UUID, val tokens: List<StateAndRef<FungibleToken>>
)

@InitiatingFlow
@StartableByRPC
class MintNFT(
    private val params: MintNFTParams, private val notaryConf: NotaryConf
) : FlowLogic<SignedTransaction>() {

    companion object {
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction for between accounts.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            GENERATING_TRANSACTION, SIGNING_TRANSACTION, GATHERING_SIGS, FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()


    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = GENERATING_TRANSACTION

        val myAccount = subFlow(UUIDToAccount(params.me.toString()))!!
        val myAccountParty = subFlow(RequestKeyForAccount(myAccount))

        val mintTransactionHash = subFlow(
            MintNFTTransaction(
                title = "DRX-recordings",
                contractId = "drx-recordings/snft.testnet",
                description = "Ownership representation of a recording",
                fundingHash = "8M17AEU85qYPVTWhk88beAXPULStLoANcchjezn1F171"
            )
        )
        val lockState = TokenLockState(
            mintTransactionHash = mintTransactionHash,
            redemptionTransactionHash = null,
            participants = listOf(myAccountParty)
        )

        val notary = serviceHub.networkMapCache.getNotary(
            CordaX500Name(
                notaryConf.organization, notaryConf.locality, notaryConf.country
            )
        )
        check(notary != null) { "Notary not found" }

        var transactionBuilder = TransactionBuilder(
            notary
        )

        transactionBuilder.addOutputState(lockState).addOutputState(lockState)
            .addCommand(DigitalRightsTokenContract.Commands.MintNFT(), listOf(myAccountParty.owningKey))
        params.tokens.forEach {
            transactionBuilder.addInputState(it)
        }

        progressTracker.currentStep = SIGNING_TRANSACTION

        val locallySignedTx = serviceHub.signInitialTransaction(
            transactionBuilder, myAccountParty.owningKey
        )

        val ownerSignatures = ArrayList<TransactionSignature>()

        progressTracker.currentStep = GATHERING_SIGS

        val tokenIssuer = params.tokens[0].state.data.issuer
        val issuerSession = initiateFlow(tokenIssuer)
        val collectedSignatures = subFlow(
            CollectSignatureFlow(
                locallySignedTx, issuerSession, tokenIssuer.owningKey
            )
        )
        ownerSignatures.addAll(collectedSignatures)

        val signedTransaction = locallySignedTx.withAdditionalSignatures(ownerSignatures)

        progressTracker.currentStep = FINALISING_TRANSACTION

        return subFlow(FinalityFlow(signedTransaction, issuerSession))
    }
}

@InitiatedBy(MintNFT::class)
class MintNFTResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionSigner = object : SignTransactionFlow(counterpartySession) {

            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) {
            }
        }

        val transaction = subFlow(transactionSigner)
        if (counterpartySession.counterparty != ourIdentity) {
            subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = transaction.id))
        }
    }
}
