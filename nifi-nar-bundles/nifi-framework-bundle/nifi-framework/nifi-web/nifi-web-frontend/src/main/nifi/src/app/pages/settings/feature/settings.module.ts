/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { Settings } from './settings.component';
import { reducers, settingsFeatureKey } from '../state';
import { SettingsRoutingModule } from './settings-routing.module';
import { GeneralEffects } from '../state/general/general.effects';
import { ManagementControllerServicesEffects } from '../state/management-controller-services/management-controller-services.effects';
import { GeneralModule } from '../ui/general/general.module';
import { ManagementControllerServicesModule } from '../ui/management-controller-services/management-controller-services.module';
import { FlowAnalysisRulesModule } from '../ui/flow-analysis-rules/flow-analysis-rules.module';
import { ParameterProvidersModule } from '../ui/parameter-providers/parameter-providers.module';
import { RegistryClientsModule } from '../ui/registry-clients/registry-clients.module';
import { ReportingTasksModule } from '../ui/reporting-tasks/reporting-tasks.module';
import { MatTabsModule } from '@angular/material/tabs';
import { ReportingTasksEffects } from '../state/reporting-tasks/reporting-tasks.effects';

@NgModule({
    declarations: [Settings],
    exports: [Settings],
    imports: [
        CommonModule,
        GeneralModule,
        ManagementControllerServicesModule,
        FlowAnalysisRulesModule,
        ParameterProvidersModule,
        RegistryClientsModule,
        ReportingTasksModule,
        SettingsRoutingModule,
        StoreModule.forFeature(settingsFeatureKey, reducers),
        EffectsModule.forFeature(GeneralEffects, ManagementControllerServicesEffects, ReportingTasksEffects),
        MatTabsModule
    ]
})
export class SettingsModule {}
