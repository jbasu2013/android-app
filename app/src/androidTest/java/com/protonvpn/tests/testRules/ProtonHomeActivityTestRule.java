/*
 * Copyright (c) 2019 Proton Technologies AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.tests.testRules;

import com.protonvpn.MockSwitch;
import com.protonvpn.android.models.config.VpnProtocol;
import com.protonvpn.android.ui.home.HomeActivity;
import com.protonvpn.android.vpn.ErrorType;
import com.protonvpn.android.vpn.VpnState;
import com.protonvpn.testsHelper.ServiceTestHelper;
import com.protonvpn.testsHelper.UserDataHelper;

import org.junit.runner.Description;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.ServiceTestRule;

public class ProtonHomeActivityTestRule extends InstantTaskExecutorRule {

    private ServiceTestHelper service;

    public ActivityTestRule<HomeActivity> activityTestRule =
        new ActivityTestRule<>(HomeActivity.class, false, false);

    @Override
    protected void starting(Description description) {
        super.starting(description);
        service = new ServiceTestHelper();
        activityTestRule.launchActivity(null);
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        service.enableSecureCore(false);
        service.connectionManager.disconnect();
        new UserDataHelper().userData.logout();
        service.deleteCreatedProfiles();
        activityTestRule.finishActivity();
    }

    public void mockStatusOnConnect(VpnState state) {
        service.mockVpnBackend.setStateOnConnect(state);
    }

    public void mockErrorOnConnect(ErrorType type) {
        service.mockVpnBackend.setStateOnConnect(new VpnState.Error(type, null));
    }

    public HomeActivity getActivity() {
        return activityTestRule.getActivity();
    }
}
