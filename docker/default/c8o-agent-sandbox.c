/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

/*
 * c8o-agent-sandbox: runs an AI agent of the engine (or one of its commands) confined by Landlock to the folders it
 * is given, without new privileges (no sudo, no setuid), see engine.util.AgentSandbox.
 *
 *   c8o-agent-sandbox --probe                                  prints the Landlock version of the kernel, exit 0
 *   c8o-agent-sandbox [--ro PATH]... [--rw PATH]... -- COMMAND [ARG]...
 *
 * --ro: read and run (files, directories and what they hold), --rw: every file system right. Anything else is
 * refused, for this process and every process it starts. A path that does not exist is left out. It never runs the
 * command unconfined: without Landlock it exits 3.
 *
 * Landlock limits the file system only (ABI 1 to 3 handled, IOCTL_DEV, the network and the scopes are not): the
 * command still reaches the network.
 */

#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <linux/types.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>

/* the same numbers on every architecture (generic syscall table), kept here for older headers */
#ifndef __NR_landlock_create_ruleset
#define __NR_landlock_create_ruleset 444
#endif
#ifndef __NR_landlock_add_rule
#define __NR_landlock_add_rule 445
#endif
#ifndef __NR_landlock_restrict_self
#define __NR_landlock_restrict_self 446
#endif

#define CREATE_RULESET_VERSION (1U << 0)
#define RULE_PATH_BENEATH 1

#define FS_EXECUTE (1ULL << 0)
#define FS_WRITE_FILE (1ULL << 1)
#define FS_READ_FILE (1ULL << 2)
#define FS_READ_DIR (1ULL << 3)
#define FS_REMOVE_DIR (1ULL << 4)
#define FS_REMOVE_FILE (1ULL << 5)
#define FS_MAKE_CHAR (1ULL << 6)
#define FS_MAKE_DIR (1ULL << 7)
#define FS_MAKE_REG (1ULL << 8)
#define FS_MAKE_SOCK (1ULL << 9)
#define FS_MAKE_FIFO (1ULL << 10)
#define FS_MAKE_BLOCK (1ULL << 11)
#define FS_MAKE_SYM (1ULL << 12)
#define FS_REFER (1ULL << 13)    /* ABI 2 */
#define FS_TRUNCATE (1ULL << 14) /* ABI 3 */

#define FS_ABI1 ((FS_MAKE_SYM << 1) - 1)
/* the rights a rule on a file (not a directory) may hold */
#define FS_ON_FILE (FS_EXECUTE | FS_WRITE_FILE | FS_READ_FILE | FS_TRUNCATE)
#define FS_READ_ONLY (FS_EXECUTE | FS_READ_FILE | FS_READ_DIR)

struct ruleset_attr {
	__u64 handled_access_fs;
	__u64 handled_access_net;
	__u64 scoped;
};

struct path_beneath_attr {
	__u64 allowed_access;
	__s32 parent_fd;
} __attribute__((packed));

static int landlock_abi(void) {
	long abi = syscall(__NR_landlock_create_ruleset, NULL, 0, CREATE_RULESET_VERSION);
	return abi < 0 ? 0 : (int) abi;
}

static int usage(void) {
	fprintf(stderr, "usage: c8o-agent-sandbox --probe | [--ro PATH]... [--rw PATH]... -- COMMAND [ARG]...\n");
	return 2;
}

/* 0, or 1 when the path is left out (missing), -1 on an error */
static int add_rule(int ruleset, const char *path, __u64 access, __u64 handled) {
	int fd = open(path, O_PATH | O_CLOEXEC);
	if (fd < 0) {
		if (errno == ENOENT || errno == ENOTDIR) {
			return 1;
		}
		fprintf(stderr, "c8o-agent-sandbox: %s: %s\n", path, strerror(errno));
		return -1;
	}
	struct stat st;
	if (fstat(fd, &st) != 0) {
		fprintf(stderr, "c8o-agent-sandbox: %s: %s\n", path, strerror(errno));
		close(fd);
		return -1;
	}
	struct path_beneath_attr rule = {
		.allowed_access = access & handled & (S_ISDIR(st.st_mode) ? ~0ULL : FS_ON_FILE),
		.parent_fd = fd,
	};
	int added = (int) syscall(__NR_landlock_add_rule, ruleset, RULE_PATH_BENEATH, &rule, 0);
	int error = errno;
	close(fd);
	if (added != 0) {
		fprintf(stderr, "c8o-agent-sandbox: %s: %s\n", path, strerror(error));
		return -1;
	}
	return 0;
}

int main(int argc, char **argv) {
	if (argc == 2 && strcmp(argv[1], "--probe") == 0) {
		int abi = landlock_abi();
		printf("%d\n", abi);
		return abi > 0 ? 0 : 3;
	}
	int command = 0;
	for (int i = 1; i < argc; i++) {
		if (strcmp(argv[i], "--") == 0) {
			command = i + 1;
			break;
		}
		if ((strcmp(argv[i], "--ro") != 0 && strcmp(argv[i], "--rw") != 0) || i + 1 >= argc || argv[i + 1][0] != '/') {
			return usage();
		}
		i++;
	}
	if (command == 0 || command >= argc) {
		return usage();
	}

	int abi = landlock_abi();
	if (abi < 1) {
		fprintf(stderr, "c8o-agent-sandbox: the kernel has no Landlock, the command is not run\n");
		return 3;
	}
	__u64 handled = FS_ABI1 | (abi >= 2 ? FS_REFER : 0) | (abi >= 3 ? FS_TRUNCATE : 0);
	struct ruleset_attr attr = { .handled_access_fs = handled };
	int ruleset = (int) syscall(__NR_landlock_create_ruleset, &attr, sizeof(attr), 0);
	if (ruleset < 0 && errno == E2BIG) {
		/* a kernel older than the scopes reads the first fields only */
		ruleset = (int) syscall(__NR_landlock_create_ruleset, &attr, sizeof(attr.handled_access_fs), 0);
	}
	if (ruleset < 0) {
		fprintf(stderr, "c8o-agent-sandbox: landlock_create_ruleset: %s\n", strerror(errno));
		return 3;
	}
	for (int i = 1; i < command - 1; i += 2) {
		__u64 access = strcmp(argv[i], "--rw") == 0 ? handled : FS_READ_ONLY;
		if (add_rule(ruleset, argv[i + 1], access, handled) < 0) {
			return 3;
		}
	}
	if (prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) {
		fprintf(stderr, "c8o-agent-sandbox: no_new_privs: %s\n", strerror(errno));
		return 3;
	}
	if (syscall(__NR_landlock_restrict_self, ruleset, 0) != 0) {
		fprintf(stderr, "c8o-agent-sandbox: landlock_restrict_self: %s\n", strerror(errno));
		return 3;
	}
	close(ruleset);
	execvp(argv[command], &argv[command]);
	fprintf(stderr, "c8o-agent-sandbox: %s: %s\n", argv[command], strerror(errno));
	return 127;
}
