/**
 * TIET Student Portal - Thapar Institute of Engineering & Technology
 * Client-Side Application Script
 */

// Show / Hide Password toggle
function togglePasswordVisibility(inputId, btn) {
    var input = document.getElementById(inputId);
    if (!input) return;

    if (input.type === "password") {
        input.type = "text";
        btn.innerHTML = "&#128065;"; // open eye
        btn.setAttribute("aria-label", "Hide password");
        btn.title = "Hide password";
    } else {
        input.type = "password";
        btn.innerHTML = "&#128064;"; // eye
        btn.setAttribute("aria-label", "Show password");
        btn.title = "Show password";
    }
}

// Student Form Validation
function validateStudentForm(form) {
    var rollNumber = form.rollNumber ? form.rollNumber.value.trim() : "";
    var name = form.name ? form.name.value.trim() : "";
    var email = form.email ? form.email.value.trim() : "";
    var phone = form.phone ? form.phone.value.trim() : "";
    var department = form.department ? form.department.value.trim() : "";
    var semester = form.semester ? form.semester.value.trim() : "";
    var course = form.course ? form.course.value.trim() : "";

    if (!rollNumber) {
        alert("Please enter the student's Roll Number / Registration ID.");
        if (form.rollNumber) form.rollNumber.focus();
        return false;
    }

    if (!name) {
        alert("Please enter the student's Full Name.");
        if (form.name) form.name.focus();
        return false;
    }

    if (!email) {
        alert("Please enter a valid official/academic Email Address.");
        if (form.email) form.email.focus();
        return false;
    }

    var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        alert("Please enter a valid format for email address (e.g. name@thapar.edu).");
        if (form.email) form.email.focus();
        return false;
    }

    if (!phone) {
        alert("Please enter a contact phone number.");
        if (form.phone) form.phone.focus();
        return false;
    }

    if (!course) {
        alert("Please select the enrolled academic programme / course.");
        if (form.course) form.course.focus();
        return false;
    }

    if (!department) {
        alert("Please select the academic department.");
        if (form.department) form.department.focus();
        return false;
    }

    if (!semester) {
        alert("Please select the current semester.");
        if (form.semester) form.semester.focus();
        return false;
    }

    if (form.cgpa) {
        var cgpaVal = parseFloat(form.cgpa.value);
        if (isNaN(cgpaVal) || cgpaVal < 0 || cgpaVal > 10) {
            alert("CGPA must be a valid numeric score between 0.00 and 10.00 (TIET 10-point scale).");
            form.cgpa.focus();
            return false;
        }
    }

    return true;
}

// Student record deletion confirmation dialog
function confirmDelete(studentName, rollNumber) {
    return confirm(
        "Are you sure you want to permanently delete this student record from the TIET database?\n\n" +
        "Student Name: " + studentName + "\n" +
        "Roll Number:  " + rollNumber + "\n\n" +
        "This operation cannot be reversed."
    );
}

// Login Page Role Switching
function switchLoginRole(role) {
    var roleInput = document.getElementById("loginRole");
    var adminTab = document.getElementById("adminTab");
    var studentTab = document.getElementById("studentTab");
    var usernameLabel = document.getElementById("usernameLabel");
    var passwordLabel = document.getElementById("passwordLabel");
    var usernameInput = document.getElementById("usernameInput");
    var passwordInput = document.getElementById("passwordInput");
    var studentRegisterBox = document.getElementById("studentRegisterBox");
    var pwdToggleBtn = document.getElementById("pwdToggleBtn");

    if (!roleInput || !adminTab || !studentTab) return;

    if (role === "student") {
        roleInput.value = "student";
        studentTab.classList.add("active");
        adminTab.classList.remove("active");

        if (usernameLabel) usernameLabel.innerHTML = "Roll Number <span class='required'>*</span>";
        if (passwordLabel) passwordLabel.innerHTML = "Registered Email / Password <span class='required'>*</span>";
        if (usernameInput) usernameInput.placeholder = "Enter your roll number (e.g. 102103045 / CS202401)";
        if (passwordInput) {
            passwordInput.placeholder = "Enter your registered email address";
            passwordInput.type = "text";
        }
        if (pwdToggleBtn) pwdToggleBtn.style.display = "none";
        if (studentRegisterBox) studentRegisterBox.style.display = "block";
    } else {
        roleInput.value = "admin";
        adminTab.classList.add("active");
        studentTab.classList.remove("active");

        if (usernameLabel) usernameLabel.innerHTML = "Admin Username <span class='required'>*</span>";
        if (passwordLabel) passwordLabel.innerHTML = "Security Password <span class='required'>*</span>";
        if (usernameInput) usernameInput.placeholder = "Enter administrator username";
        if (passwordInput) {
            passwordInput.placeholder = "Enter administrative password";
            passwordInput.type = "password";
        }
        if (pwdToggleBtn) {
            pwdToggleBtn.style.display = "flex";
            pwdToggleBtn.innerHTML = "&#128064;";
        }
        if (studentRegisterBox) studentRegisterBox.style.display = "none";
    }
}
