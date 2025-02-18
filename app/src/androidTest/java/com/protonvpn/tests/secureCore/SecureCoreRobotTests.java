/*
 * Copyright (c) 2018 Proton Technologies AG
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
package com.protonvpn.tests.secureCore;

import com.protonvpn.MockSwitch;
import com.protonvpn.actions.CountriesRobot;
import com.protonvpn.actions.HomeRobot;
import com.protonvpn.actions.MapRobot;
import com.protonvpn.android.vpn.VpnState;
import com.protonvpn.results.ConnectionResult;
import com.protonvpn.results.MapResult;
import com.protonvpn.test.shared.TestUser;
import com.protonvpn.tests.testRules.ProtonHomeActivityTestRule;
import com.protonvpn.tests.testRules.SetUserPreferencesRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@LargeTest
@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
public class SecureCoreRobotTests {

    private final ProtonHomeActivityTestRule testRule = new ProtonHomeActivityTestRule();
    @Rule public RuleChain rules = RuleChain
        .outerRule(new HiltAndroidRule(this))
        .around(new SetUserPreferencesRule(TestUser.getPlusUser()))
        .around(testRule);

    private HomeRobot homeRobot;

    @Before
    public void setup() {
        homeRobot = new HomeRobot();
    }

    @Test
    public void connectAndDisconnectFromSecureCoreThroughMap() {
        testRule.mockStatusOnConnect(VpnState.Connected.INSTANCE);

        MapRobot map = homeRobot.clickOnMapViewTab();

        map.enableSecureCore();
        map.clickOnSecureCoreSwedenNode();

        MapResult mapResult = new MapResult();
        mapResult.isSwedenNodeSelected();

        map.clickOnSecureCoreFranceNode();

        ConnectionResult result = map.clickConnectButton();
        result.isConnectedToVpn();

        result.connectionRobot.clickDisconnectButton();
        result.isDisconnectedFromVpn();
    }

    @Test
    public void connectAndDisconnectFromSecureCoreThroughQuickConnect() {
        testRule.mockStatusOnConnect(VpnState.Connected.INSTANCE);

        homeRobot.enableSecureCore();
        ConnectionResult result = homeRobot.connectThroughQuickConnect();
        result.isConnectedToVpn();

        result.connectionRobot.clickDisconnectButton();
        result.isDisconnectedFromVpn();
    }

    @Test
    public void connectAndDisconnectFromSecureCoreThroughCountryList() {
        testRule.mockStatusOnConnect(VpnState.Connected.INSTANCE);

        homeRobot.enableSecureCore();
        CountriesRobot countries = homeRobot.clickOnCountriesTab();
        String country = MockSwitch.mockedServersUsed ? "Finland" : "Australia";
        countries.selectCountry(country);

        ConnectionResult result = countries.clickConnectButton("via Sweden");
        result.isConnectedToVpn();

        result.connectionRobot.clickDisconnectButton();
        result.isDisconnectedFromVpn();
    }
}
