package com.example.nori_tura

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nori_tura.presentation.admin.AdminHomeScreen
import com.example.nori_tura.presentation.auth.AuthUiState
import com.example.nori_tura.presentation.auth.AuthViewModel
import com.example.nori_tura.presentation.auth.LoginScreen
import com.example.nori_tura.presentation.auth.RegisterDoctorScreen
import com.example.nori_tura.presentation.auth.VerifyOtpScreen
import com.example.nori_tura.presentation.home.NurseHomeScreen
import com.example.nori_tura.presentation.home.ParentHomeScreen
import com.example.nori_tura.presentation.ipd.AdmissionsListScreen
import com.example.nori_tura.presentation.ipd.AdmissionDetailScreen
import com.example.nori_tura.presentation.opd.OpdConsultScreen
import com.example.nori_tura.presentation.surgeon.AddPatientScreen
import com.example.nori_tura.presentation.surgeon.PatientListScreen
import com.example.nori_tura.presentation.surgeon.PatientProfileScreen
import com.example.nori_tura.presentation.surgeon.SurgeonDashboardScreen
import com.example.nori_tura.presentation.surgeon.SurgicalTemplatesScreen

@Composable
fun App(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    MaterialTheme {
        val uiState by authViewModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            authViewModel.checkAuthStatus()
        }

        LaunchedEffect(uiState) {
            when (val state = uiState) {
                is AuthUiState.Authenticated -> {
                    val destination = when (state.role.lowercase()) {
                        "surgeon" -> "surgeon_home"
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

            composable("surgeon_home") {
                SurgeonDashboardScreen(
                    onNavigateToPatientList = { navController.navigate("patient_list") },
                    onNavigateToAppointments = { navController.navigate("appointments") },
                    onNavigateToSurgicalTemplates = { navController.navigate("surgical_templates") },
                    onNavigateToAdmissions = { navController.navigate("admissions") },
                    onNavigateToPatientProfile = { patientId ->
                        navController.navigate("patient_profile/$patientId")
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
                    onPatientAdded = {
                        navController.popBackStack()
                    }
                )
            }

            composable("patient_profile/{patientId}") { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                PatientProfileScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                    onAddOpdRecord = { navController.navigate("opd_consult/$patientId") }
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
                Text(text = "Appointments screen - coming soon")
            }

            composable("surgical_templates") {
                SurgicalTemplatesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("admissions") {
                AdmissionsListScreen(
                    patients = emptyList(),
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
                    onBack = { navController.popBackStack() }
                )
            }

            composable("nurse_home") {
                NurseHomeScreen(
                    onNavigateToPatientList = { navController.navigate("patient_list") },
                    onNavigateToAddPatient = { navController.navigate("add_patient") },
                    onNavigateToAppointments = { navController.navigate("appointments") }
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
                ParentHomeScreen()
            }
        }
    }
}
