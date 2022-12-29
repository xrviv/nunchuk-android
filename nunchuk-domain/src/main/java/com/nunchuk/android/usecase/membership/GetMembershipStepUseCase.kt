package com.nunchuk.android.usecase.membership

import com.nunchuk.android.FlowUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.repository.MembershipRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMembershipStepUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: MembershipRepository,
) : FlowUseCase<MembershipPlan, List<MembershipStepInfo>>(dispatcher) {
    override fun execute(parameters: MembershipPlan): Flow<List<MembershipStepInfo>> {
        return repository.getSteps(parameters)
    }
}