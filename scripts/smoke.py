"""Exercise the running API with a disposable student account.

Run after starting the backend: python scripts/smoke.py
"""

import json
import os
import urllib.error
import urllib.request
import uuid

BASE = os.environ.get("COURSE_REC_API_BASE", "http://localhost:8080/api")


def call(method, path, body=None, token=None, expected=200):
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode() if body is not None else None,
        headers=headers,
        method=method,
    )
    try:
        with urllib.request.urlopen(request) as response:
            status = response.status
            payload = response.read()
    except urllib.error.HTTPError as error:
        status = error.code
        payload = error.read()
    if status != expected:
        raise AssertionError(f"{method} {path}: expected {expected}, got {status}: {payload!r}")
    return json.loads(payload) if payload else None


def main():
    email = f"smoke-{uuid.uuid4().hex}@example.test"
    auth = call("POST", "/auth/register", {"name": "Smoke Student", "email": email, "password": "test-password-123"})
    token = auth["token"]
    try:
        profile = call("PUT", "/profile", {
            "name": "Smoke Student",
            "academicBackground": "Computer Science",
            "proficiencyLevel": "BEGINNER",
            "careerGoal": "Machine Learning Engineer",
            "interests": ["Python", "Machine Learning"],
            "skills": [{"name": "Python", "proficiency": "BEGINNER"}],
        }, token)
        assert profile["careerGoal"] == "Machine Learning Engineer"

        courses = call("GET", "/courses")
        filtered = call("GET", "/courses?topic=Python")
        recommendations = call("GET", "/recommendations", token=token)
        assert len(courses) >= 12 and len(filtered) >= 1 and len(recommendations) == len(courses)
        assert recommendations[0]["score"] >= recommendations[-1]["score"]

        top = recommendations[0]
        course_id = top["course"]["id"]
        call("POST", f"/saved/{course_id}", token=token, expected=204)
        call("POST", f"/saved/{course_id}", token=token, expected=204)
        saved = call("GET", "/saved", token=token)
        assert len(saved) == 1 and saved[0]["id"] == course_id
        call("DELETE", f"/saved/{course_id}", token=token, expected=204)
        assert call("GET", "/saved", token=token) == []
        call("POST", "/admin/courses", {"title": "Denied"}, token, expected=403)

        print(json.dumps({
            "courses": len(courses),
            "python_courses": len(filtered),
            "recommendations": len(recommendations),
            "top_course": top["course"]["title"],
            "top_score": top["score"],
            "saved_flow": "passed",
            "admin_gate": "passed",
        }, indent=2))
    finally:
        call("DELETE", "/profile", token=token, expected=200)


if __name__ == "__main__":
    main()
