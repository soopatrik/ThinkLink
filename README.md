**ThinkLink
**

 A collaborative mind mapping and project management tool designed to enhance team productivity through visual thinking and organization.



📋 Table of Contents

Overview
Features
Project Structure
Setup & Installation
Usage Guide
Team Workflow
Future Development
Overview

ThinkLink is a Java-based desktop application that combines mind mapping with project management features. It allows users to create and share visual representations of ideas, plans, and concepts while integrating calendar, notes, and checklist functionalities to keep projects organized and on track.
Built as a collaborative tool, ThinkLink supports role-based access with different permissions for administrators and regular users, following the UML design principles outlined in the project requirements.

Features

🧠 Visual Mind Mapping
Create and connect idea boxes with intuitive drag-and-drop
Link related concepts with directional arrows
Customize box appearance with colors and text

📆 Integrated Calendar
Track deadlines and important dates
View deadlines by month
Administrator-exclusive deadline management

✅ Task Management
Create and manage goal-oriented checklists
Track progress with completion status
Organize tasks by priority

📝 Note Taking
Create, edit, and organize personal notes
Rich text formatting support
Private note management for each user

👥 Collaboration Features
User management with role-based permissions
Administrators can create and manage shared boards
Regular users can create and edit personal content

🔒 Security
User authentication system
Role-based access control (Administrator/Customary)
Private data protection



**Setup & Installation
**


Prerequisites
Java Development Kit (JDK) 11 or newer
Git (for version control)
Installation Steps

1. Clone the repository
   git clone https://github.com/soopatrik/ThinkLink.git
   cd ThinkLink

2. Compile the project
   # Create bin directory
   mkdir -p bin
   
   # Compile all Java files
   find src -name "*.java" | xargs javac -d bin

3. Run the application
     java -cp bin main.java.application.ThinkLink


Usage Guide

Login
1. Launch the application
2. Enter any username
3. Select either "Regular User" or "Administrator" role
4. Click "Login"
   
Mind Mapping
- Create Box: Double-click on the canvas
- Move Box: Click and drag a box
- Connect Boxes: Click on source box, then shift+click on target box
- Select Box: Click on a box (selected box displays with red outline)
- Delete Box: Select a box and press Delete key

Calendar
- Navigate between months using the arrows
- Administrators can set deadlines by clicking "Set Deadline"
- View all deadlines by clicking "View All Deadlines"

Checklists
- Add goals by typing in the input field and clicking "Add"
- Toggle completion by clicking on a goal
- Remove goals using the "Remove Selected" button
- Clear completed items with "Clear Completed" button

Notes
- Create new notes with the "New Note" button
- Edit title and content in the right panel
- Save changes with the "Save" button
- Delete notes with the "Delete" button

© 2025 ThinkLink Team
