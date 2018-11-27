const getip = function getip(servicename) {
  let res = fetch(`http://149.165.157.99:8081/service/${servicename}`, {
    method: "GET"
  })
    .then(res => {
      if (res.ok) {
        return res.json();
      }
    })
    .then(result => {
      if (result != undefined) {
        console.log("result in getip for login", result);
        return result;
      } else {
        return "Failed";
      }
    })
    .catch(error => {
      console.log(`error on ${servicename}`);
      return "Failed";
    });
  return res;
};

export { getip };
