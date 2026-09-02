package com.nonituracare

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nonituracare.presentation.appointments.AppointmentCallScreen
import com.nonituracare.presentation.appointments.AppointmentRequestScreen
import com.nonituracare.presentation.appointments.AppointmentViewModel
import com.nonituracare.presentation.medicalrecords.MedicalRecordDetailScreen
import com.nonituracare.presentation.medicalrecords.MedicalRecordsViewScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nonituracare.presentation.admin.AdminHomeScreen
import com.nonituracare.presentation.auth.AuthUiState
import com.nonituracare.presentation.auth.AuthViewModel
import com.nonituracare.presentation.auth.LoginScreen
import com.nonituracare.presentation.auth.RegisterDoctorScreen
import com.nonituracare.presentation.auth.VerifyOtpScreen
import com.nonituracare.presentation.home.NurseHomeScreen
import com.nonituracare.presentation.home.ParentHomeScreen
import com.nonituracare.presentation.parent.ParentConsultDetailScreen
import com.nonituracare.presentation.parent.ParentOpdRecordsScreen
import com.nonituracare.presentation.parent.ParentProfileScreen
import com.nonituracare.presentation.parent.SurgeryStatusScreen
import com.nonituracare.presentation.ipd.AdmissionsListScreen
import com.nonituracare.presentation.ipd.AdmissionDetailScreen
import com.nonituracare.presentation.ipd.ConsentFormScreen
import com.nonituracare.presentation.ipd.ConsentViewScreen
import com.nonituracare.presentation.opd.OpdConsultScreen
import com.nonituracare.presentation.opd.OpdRecordDetailScreen
import com.nonituracare.presentation.surgeon.AddPatientScreen
import com.nonituracare.presentation.surgeon.PatientListScreen
import com.nonituracare.presentation.surgeon.PatientProfileScreen
import com.nonituracare.presentation.surgeon.ScheduleScreen
import com.nonituracare.presentation.surgeon.SurgeonMainScreen
import com.nonituracare.presentation.surgeon.SurgicalTemplatesScreen
import com.nonituracare.presentation.surgeon.WhatsAppPreviewScreen
import com.nonituracare.ui.theme.NorituraTheme

@Composable
fun App(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    NorituraTheme {
        val uiState by authViewModel.uiState.collectAsState()
        val appointmentViewModel: AppointmentViewModel = viewModel { AppointmentViewModel() }
        var pendingDoctorId by remember { mutableStateOf("") }
        var pendingDoctorName by remember { mutableStateOf("") }
        var pendingHospitalContact by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            authViewModel.checkAuthStatus()
        }

        LaunchedEffect(uiState) {
            when (val state = uiState) {
                is AuthUiState.Authenticated -> {
                    val destination = when (state.role.lowercase()) {
                        "surgeon" -> "surgeon_main"
                        "nurse" -> "nurse_home"
                        "patient_parent" -> "parent_home"
                        "admin" -> "admin_home"
                        "superadmin" -> "superadmin_home"
                        else -> "login"
                    }
                    navController.navigate(destination) {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is AuthUiState.OtpSent -> {
                    navController.navigate("verify_otp/${state.phone}") {
                        launchSingleTop = true
                    }
                }
                is AuthUiState.RegistrationSubmitted -> {
                    navController.popBackStack()
                }
                else -> Unit
            }
        }

        NavHost(
            navController = navController,
            startDestination = "login"
        ) {
            composable("login") {
                LoginScreen(
                    uiState = uiState,
                    onSendOtp = { phone ->
                        authViewModel.resetError()
                        authViewModel.sendOtp(phone)
                    },
                    onNavigateToRegister = {
                        authViewModel.resetError()
                        navController.navigate("register_doctor")
                    }
                )
            }

            composable("register_doctor") {
                RegisterDoctorScreen(
                    onBack = { navController.popBackStack() },
                    onRegistrationSubmitted = {
                        authViewModel.resetError()
                        navController.popBackStack()
                    }
                )
            }

            composable("verify_otp/{phone}") { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                VerifyOtpScreen(
                    phone = phone,
                    uiState = uiState,
                    onVerifyOtp = { _, otp ->
                        authViewModel.resetError()
                        authViewModel.verifyOtp(phone, otp)
                    },
                    onResendOtp = { phoneNumber ->
                        authViewModel.resetError()
                        authViewModel.sendOtp(phoneNumber)
                    }
                )
            }

            composable("surgeon_main") {
                SurgeonMainScreen(
                    onNavigateToPatientProfile = { patientId ->
                        navController.navigate("patient_profile/$patientId")
                    },
                    onNavigateToAddPatient = { navController.navigate("add_patient") },
                    onNavigateToAppointments = { navController.navigate("appointments") },
                    onNavigateToSurgicalTemplates = { navController.navigate("surgical_templates") },
                    onNavigateToAdmissions = { navController.navigate("admissions") },
                    onNavigateToFollowUpPreview = { recordId ->
                        navController.navigate("follow_up_preview/$recordId")
                    },
                    onNavigateToConsentView = { consentId ->
                        navController.navigate("consent_view/$consentId")
                    },
                    onNavigateToAdmissionDetail = { admissionId ->
                        navController.navigate("admission_detail/$admissionId")
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("surgeon_main") { inclusive = true }
                        }
                    }
                )
            }

            composable("patient_list") {
                PatientListScreen(
                    onBack = { navController.popBackStack() },
                    onPatientClick = { patientId ->
                        navController.navigate("patient_profile/$patientId")
                    },
                    onAddPatient = { navController.navigate("add_patient") }
                )
            }

            composable("add_patient") {
                AddPatientScreen(
                    onBack = { navController.popBackStack() },
                    onPatientAdded = { patientId ->
                        navController.navigate("patient_profile/$patientId") {
                            popUpTo("add_patient") { inclusive = true }
                        }
                    }
                )
            }

            composable("patient_profile/{patientId}") { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                PatientProfileScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                    onAddOpdRecord = { navController.navigate("opd_consult/$patientId") },
                    onNavigateToConsentForm = { admissionId ->
                        navController.navigate("consent_form/$admissionId")
                    },
                    onNavigateToConsentView = { consentId ->
                        navController.navigate("consent_view/$consentId")
                    },
                    onNavigateToOpdRecordDetail = { recordId ->
                        navController.navigate("opd_record_detail/$recordId")
                    },
                    onNavigateToMedicalRecords = {
                        navController.navigate("medical_records/$patientId")
                    },
                    onNavigateToAdmission = { admissionId ->
                        navController.navigate("admission_detail/$admissionId")
                    }
                )
            }

            composable("opd_record_detail/{recordId}") { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
                OpdRecordDetailScreen(
                    recordId = recordId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("opd_consult/{patientId}") { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                val isNurse = (uiState as? AuthUiState.Authenticated)?.role?.lowercase() == "nurse"
                OpdConsultScreen(
                    patientId = patientId,
                    isNurse = isNurse,
                    onBack = { navController.popBackStack() },
                    onRecordSaved = { navController.popBackStack() }
                )
            }

            composable("appointments") {
                ScheduleScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("surgical_templates") {
                SurgicalTemplatesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("admissions") {
                AdmissionsListScreen(
                    onBack = { navController.popBackStack() },
                    onAdmissionClick = { admissionId ->
                        navController.navigate("admission_detail/$admissionId")
                    }
                )
            }

            composable("admission_detail/{admissionId}") { backStackEntry ->
                val admissionId = backStackEntry.arguments?.getString("admissionId") ?: ""
                AdmissionDetailScreen(
                    admissionId = admissionId,
                    onBack = { navController.popBackStack() },
                    onNavigateToConsentForm = {
                        navController.navigate("consent_form/$admissionId")
                    },
                    onNavigateToConsentView = { consentId ->
                        navController.navigate("consent_view/$consentId")
                    }
                )
            }

            composable("consent_form/{admissionId}") { backStackEntry ->
                val admissionId = backStackEntry.arguments?.getString("admissionId") ?: ""
                ConsentFormScreen(
                    admissionId = admissionId,
                    onBack = { navController.popBackStack() },
                    onConsentCreated = { consentId ->
                        navController.navigate("consent_view/$consentId") {
                            popUpTo("consent_form/$admissionId") { inclusive = true }
                        }
                    }
                )
            }

            composable("consent_view/{consentId}") { backStackEntry ->
                val consentId = backStackEntry.arguments?.getString("consentId") ?: ""
                ConsentViewScreen(
                    consentId = consentId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("follow_up_preview/{recordId}") { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
                WhatsAppPreviewScreen(
                    recordId = recordId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("nurse_home") {
                NurseHomeScreen(
                    onNavigateToPatientList = { navController.navigate("patient_list") },
                    onNavigateToAddPatient = { navController.navigate("add_patient") },
                    onNavigateToAppointments = { navController.navigate("appointments") },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("nurse_home") { inclusive = true }
                        }
                    }
                )
            }

            composable("admin_home") {
                AdminHomeScreen(
                    isSuperAdmin = false,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("admin_home") { inclusive = true }
                        }
                    }
                )
            }

            composable("superadmin_home") {
                AdminHomeScreen(
                    isSuperAdmin = true,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("superadmin_home") { inclusive = true }
                        }
                    }
                )
            }

            composable("parent_home") {
                ParentHomeScreen(
                    onNavigateToRecords = { navController.navigate("parent_records") },
                    onNavigateToSurgeryStatus = { admissionId ->
                        navController.navigate("surgery_status/$admissionId")
                    },
                    onNavigateToConsentView = { consentId ->
                        navController.navigate("parent_consent_view/$consentId")
                    },
                    onNavigateToProfile = {
                        navController.navigate("parent_profile")
                    },
                    onNavigateToBookAppointment = { doctorId, doctorName ->
                        pendingDoctorId = doctorId
                        pendingDoctorName = doctorName
                        navController.navigate("appointment_request")
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("parent_home") { inclusive = true }
                        }
                    }
                )
            }

            composable("parent_profile") {
                ParentProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToConsentView = { consentId ->
                        navController.navigate("parent_consent_view/$consentId")
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("parent_profile") { inclusive = true }
                        }
                    }
                )
            }

            composable("parent_records") {
                ParentOpdRecordsScreen(
                    onBack = { navController.popBackStack() },
                    onRecordClick = { recordId ->
                        navController.navigate("parent_consult_detail/$recordId")
                    }
                )
            }

            composable("surgery_status/{admissionId}") { backStackEntry ->
                val admissionId = backStackEntry.arguments?.getString("admissionId") ?: ""
                SurgeryStatusScreen(
                    admissionId = admissionId,
                    onBack = { navController.popBackStack() },
                    onNavigateToConsentView = { consentId ->
                        navController.navigate("parent_consent_view/$consentId")
                    }
                )
            }

            composable("parent_consult_detail/{recordId}") { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
                ParentConsultDetailScreen(
                    recordId = recordId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("parent_consent_view/{consentId}") { backStackEntry ->
                val consentId = backStackEntry.arguments?.getString("consentId") ?: ""
                ConsentViewScreen(
                    consentId = consentId,
                    onBack = { navController.popBackStack() },
                    topBarInitials = "PT",
                    topBarTitle = "Review & Sign Consent"
                )
            }

            composable("appointment_request") {
                AppointmentRequestScreen(
                    doctorId = pendingDoctorId,
                    doctorName = pendingDoctorName,
                    viewModel = appointmentViewModel,
                    onBack = {
                        appointmentViewModel.reset()
                        navController.popBackStack()
                    },
                    onRequested = { hospitalContact ->
                        pendingHospitalContact = hospitalContact
                        navController.navigate("appointment_call")
                    }
                )
            }

            composable("appointment_call") {
                AppointmentCallScreen(
                    hospitalContact = pendingHospitalContact,
                    onBackToHome = {
                        appointmentViewModel.reset()
                        navController.navigate("parent_home") {
                            popUpTo("parent_home") { inclusive = false }
                        }
                    }
                )
            }

            composable("medical_records/{patientId}") { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                MedicalRecordsViewScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                    onRecordClick = { recordId ->
                        navController.navigate("medical_record_detail/$recordId")
                    }
                )
            }

            composable("medical_record_detail/{recordId}") { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
                MedicalRecordDetailScreen(
                    recordId = recordId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
