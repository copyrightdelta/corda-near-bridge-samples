package com.copyrightdelta.drx.contracts.music.splits.states

import com.copyrightdelta.drx.contracts.music.recording.states.RecordingState
import com.copyrightdelta.drx.contracts.music.splits.contracts.DigitalRightsTokenContract
import com.copyrightdelta.drx.contracts.music.splits.schemas.DRTSchemaV1
import com.copyrightdelta.drx.contracts.music.work.schemas.WorkSchemaV1
import com.copyrightdelta.drx.contracts.music.work.states.WorkState
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.QueryableState

@BelongsToContract(DigitalRightsTokenContract::class)
data class DigitalRightsTokenType(

    val work: UniqueIdentifier?,
    //OR
    val recording: UniqueIdentifier?,

    override val linearId: UniqueIdentifier = UniqueIdentifier(),

    /**
     * Divisibility of the token.
     */
    override val fractionDigits: Int = 16,
    override val maintainers: List<Party>
) : EvolvableTokenType(), QueryableState {

    override fun supportedSchemas() = listOf(WorkSchemaV1)

    override fun generateMappedObject(schema: MappedSchema) =
        DRTSchemaV1.PersistentDRT(
            workId = work?.id.toString(),
            recordingId = recording?.id.toString()
        )


}