package com.copyrightdelta.drx.contracts.music.splits.contracts

import com.copyrightdelta.drx.contracts.music.splits.states.DigitalRightsTokenType
import com.copyrightdelta.drx.contracts.music.splits.states.TokenLockState
import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class DigitalRightsTokenContract : EvolvableTokenContract(), Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.copyrightdelta.drx.contracts.music.splits.DigitalRightsTokenContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.MintNFT -> verifyMintNFT(tx, command.signers)
        }
    }

    private fun verifyMintNFT(tx: LedgerTransaction, signers: List<PublicKey>) {
        requireThat {
            val tokensToTransfer = tx.inputsOfType<FungibleToken>()
            require(tokensToTransfer.isNotEmpty()) { "There must be tokens to lock." }
            val lockedTokens = tx.outputsOfType<FungibleToken>().singleOrNull()
            val lockState = tx.outputsOfType<TokenLockState>().singleOrNull()

            require(lockedTokens != null) { "There must be encumbered tokens as output." }
            require(lockState != null) { "There must be a lock state as output." }

            "Token participants must be also the lock state participants" using (lockState.participants.contains(
                lockedTokens.holder
            ))
            "All of the participants must be signers." using (signers.containsAll(lockState.participants.map { it.owningKey }))

            val tokenEncumbrance = tx.outputs.first { it.data is FungibleToken }.encumbrance
            "Tokens need to be encumbered".using(tokenEncumbrance != null)
        }
    }

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        val token = tx.outputStates.single() as DigitalRightsTokenType
        token.apply {
            require(fractionDigits == 16) { "Fractions must equal 16." }
            require((work != null && recording == null) || (work == null && recording != null)) { "Must be linked to a recording or work." }
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val oldToken = tx.inputStates.single() as DigitalRightsTokenType
        val newToken = tx.outputStates.single() as DigitalRightsTokenType
        require(oldToken.fractionDigits == newToken.fractionDigits) { "The fractions cannot change." }
        require(newToken.work !== null || newToken.recording !== null) { "Must be linked to a recording or work." }
        require(oldToken.work !== newToken.work) { "Link with work or recording cannot be changed." }
        require(oldToken.recording !== newToken.recording) { "Link with work or recording cannot be changed." }
    }

    interface Commands : CommandData {
        class MintNFT : TypeOnlyCommandData(), Commands
    }
}