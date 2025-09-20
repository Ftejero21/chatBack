package com.chat.chat.Controller;


import com.chat.chat.DTO.InviteDecisionDTO;
import com.chat.chat.Service.GroupInviteService.GroupInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/group-invites")
@CrossOrigin("*")
public class GroupInviteController {

    @Autowired
    private GroupInviteService groupInviteService;

    @PostMapping("/{inviteId}/accept")
    public void accept(@PathVariable Long inviteId, @RequestBody InviteDecisionDTO body) {
        groupInviteService.accept(inviteId, body.getUserId());
    }

    @PostMapping("/{inviteId}/decline")
    public void decline(@PathVariable Long inviteId, @RequestBody InviteDecisionDTO body) {
        groupInviteService.decline(inviteId, body.getUserId());
    }
}
