#!/bin/sh
# Copyright (c) 2001-2026 Convertigo SA.
#
# Checks c8o-agent-sandbox on a Linux kernel: builds the agent-sandbox stage of docker/default/Dockerfile, then runs
# the checks below in a throwaway container, as a user with the passwordless sudo the Convertigo image gives its
# 'convertigo' user. Run it after ./gradlew generateDockerfile:
#
#   docker/c8o-agent-sandbox-test.sh
set -eu

cd "$(dirname "$0")/default"
image=c8o-agent-sandbox-test
docker build -q --target agent-sandbox -t "$image" . >/dev/null
trap 'docker rmi -f "$image" >/dev/null 2>&1 || true' EXIT

docker run --rm -i "$image" sh -s <<'CHECKS'
set -u
apt-get update -qq >/dev/null && apt-get install -y -qq --no-install-recommends sudo >/dev/null
useradd -m agent
echo "agent ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/agent
chmod 0440 /etc/sudoers.d/agent
mkdir -p /work/ro /work/rw /work/other /usr/local/engine
echo hello > /work/ro/file
echo secret > /work/other/secret
echo engine > /usr/local/engine/file
chown -R agent:agent /work /usr/local/engine
cp /c8o-agent-sandbox /usr/local/bin/

cat > /home/agent/checks.sh <<'AGENT'
failed=0
check() { # name, expected exit (0 or "fail"), command...
	name=$1; expected=$2; shift 2
	if "$@" >/tmp/out 2>&1; then got=0; else got=fail; fi
	if [ "$got" = "$expected" ]; then echo "PASS $name"; else echo "FAIL $name ($(head -c 200 /tmp/out))"; failed=1; fi
}
# the read-only system of engine.util.AgentSandbox
S="c8o-agent-sandbox"
for p in /bin /sbin /lib /lib32 /lib64 /usr/bin /usr/sbin /usr/lib /usr/lib32 /usr/lib64 /usr/libexec /usr/share /etc /sys /dev /proc/self /proc/thread-self /proc/cpuinfo /proc/meminfo /proc/stat /proc/loadavg /proc/uptime /proc/version /proc/filesystems /proc/sys; do S="$S --ro $p"; done
S="$S --rw /dev/null --ro /work/ro --rw /work/rw --ro /does/not/exist"

abi=$(c8o-agent-sandbox --probe)
echo "landlock abi: $abi"
check "probe" 0 c8o-agent-sandbox --probe
check "reads a read-only folder" 0 $S -- cat /work/ro/file
check "writes a read-write folder" 0 $S -- sh -c 'echo x > /work/rw/f && cat /work/rw/f && rm /work/rw/f'
check "writes /dev/null" 0 $S -- sh -c 'echo x > /dev/null'
check "does not write a read-only folder" fail $S -- sh -c 'echo x > /work/ro/g'
check "does not read another folder" fail $S -- cat /work/other/secret
check "does not list another folder" fail $S -- ls /work/other
check "a child does not read it either" fail $S -- sh -c 'sh -c "cat /work/other/secret"'
check "does not read /usr/local (Tomcat, the engine)" fail $S -- cat /usr/local/engine/file
check "sudo works outside" 0 sudo -n true
check "sudo does not work inside (no_new_privs)" fail $S -- sudo -n true
sleep 30 & outside=$!
check "reads its own /proc entries" 0 $S -- cat /proc/self/environ
check "reads the system-wide /proc files" 0 $S -- cat /proc/cpuinfo /proc/meminfo
check "does not read the environment of a process outside" fail $S -- cat /proc/$outside/environ
check "does not read the command line of a process outside" fail $S -- cat /proc/$outside/cmdline
check "does not list the processes" fail $S -- ls /proc
kill $outside
check "no command" fail c8o-agent-sandbox --ro /etc --
check "a relative path" fail c8o-agent-sandbox --ro etc -- true
check "an unknown option" fail c8o-agent-sandbox --net -- true
check "a missing command" fail $S -- /nonexistent
exit $failed
AGENT
chown agent /home/agent/checks.sh
su agent -s /bin/sh -c 'sh /home/agent/checks.sh'
CHECKS
